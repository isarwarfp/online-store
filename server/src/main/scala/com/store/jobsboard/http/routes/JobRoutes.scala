package com.store.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.effect.*
import cats.implicits.*
import cats.data.Kleisli
import cats.effect.IO
import com.store.foundations.Http4s.{courseRoutes, healthEndpoint}
import tsec.authentication.asAuthed

import org.typelevel.log4cats.Logger
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.*
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Request, Response}

import com.store.jobsboard.core.*
import com.store.jobsboard.domain.job.*
import com.store.jobsboard.domain.pagination.*
import com.store.jobsboard.domain.security.*
import com.store.jobsboard.http.validation.syntax.*
import com.store.jobsboard.http.responses.FailureResponse
import com.store.jobsboard.logging.syntax.*

import java.util.UUID
import com.store.jobsboard.domain.security.Authenticator
import tsec.authentication.SecuredRequestHandler
import com.store.jobsboard.domain.user.User
import com.store.jobsboard.domain.security.JwtToken

import scala.language.implicitConversions

class JobRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (jobs: Jobs[F]) extends HttpValidationDsl[F]:
  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")

  // POST /jobs?limit=x&offset=y { filters } // TODO add query params and filter
  private val all: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset) => for {
      filter   <- req.as[JobFilter]
      jobsList <- jobs.all(filter, Pagination(limit, offset))
      resp     <- Ok(jobsList)
    } yield resp
  }

  // GET /jobs/uuid
  private val find: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) => for {
      job  <- jobs.find(id)
      resp <- job match
        case Some(job) => Ok(job)
        case None      => NotFound(FailureResponse(s"Job with $id not found"))
    } yield resp
  }

  // POST /jobs/create { jobsInfo }
  private val create: AuthRoute[F] = { case req @ POST -> Root / "create" asAuthed _ =>
    req.request.validateAs[JobInfo] { jobInfo =>
      for {
        jobId   <- jobs.create("abc@gmail.com", jobInfo)
        resp    <- Created(jobId)
      } yield resp
    }
  }

  // PUT /jobs/uuid { jobInfo }
  private val update: AuthRoute[F] = {
    case req @ PUT -> Root / UUIDVar(id) asAuthed user =>
      req.request.validateAs[JobInfo] { jobInfo =>
        jobs.find(id).flatMap {
        case None => NotFound(FailureResponse(s"Cannot update job $id not found"))
        case Some(job) if user.owns(job) || user.isAdmin => 
          jobs.update(id, jobInfo) *> Ok()
        case _ => Forbidden(FailureResponse(s"You can only delete your own jobs"))
      }
    }
  }

  // DELETE /jobs/uuid
  private val delete: AuthRoute[F] = {
    case DELETE -> Root / UUIDVar(id) asAuthed user =>
      jobs.find(id).flatMap {
        case None => NotFound(FailureResponse(s"Cannot delete job $id not found"))
        case Some(job) if user.owns(job) || user.isAdmin => 
          jobs.delete(id) *> Ok()
        case _ => Forbidden(FailureResponse(s"You can only delete your own jobs"))
      }
  }

  val unAuthedRoutes = all <+> find
  val authedRoutes = SecuredHandler[F].liftService(
    create.restrictedTo(allRoles) |+|
    update.restrictedTo(allRoles) |+|
    delete.restrictedTo(allRoles)
  )
  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (unAuthedRoutes <+> authedRoutes)
  )

object JobRoutes:
  def apply[F[_]: Concurrent: Logger: SecuredHandler](jobs: Jobs[F]) = new JobRoutes[F](jobs)
