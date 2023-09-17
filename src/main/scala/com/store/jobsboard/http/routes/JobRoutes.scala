package com.store.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.effect.*
import cats.implicits.*
import cats.data.Kleisli
import cats.effect.IO
import com.store.foundations.Http4s.{courseRoutes, healthEndpoint}

import org.typelevel.log4cats.Logger
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.*
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Request, Response}

import com.store.jobsboard.core.*
import com.store.jobsboard.domain.job.*
import com.store.jobsboard.http.validation.syntax.*
import com.store.jobsboard.http.responses.FailureResponse
import com.store.jobsboard.logging.syntax.*

import java.util.UUID

class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends HttpValidationDsl[F]:

  // POST /jobs?offset=x&limit=y { filters } // TODO add query params and filter
  private val all: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root => for {
      jobsList <- jobs.all()
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
  private val create: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      req.validateAs[JobInfo] { jobInfo =>
        for {
          jobId   <- jobs.create("abc@gmail.com", jobInfo)
          resp    <- Created(jobId)
        } yield resp
      }
  }

  // PUT /jobs/uuid { jobInfo }
  private val update: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      req.validateAs[JobInfo] { jobInfo =>
        for {
          updated  <- jobs.update(id, jobInfo)
          resp     <- updated match {
            case Some(_) => Ok()
            case None    => NotFound(FailureResponse(s"Cannot update job $id not found"))
          }
        } yield resp
      }
  }

  // DELETE /jobs/uuid
  private val delete: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) =>
      jobs.find(id).flatMap {
        case Some(_) => for {
          _ <- jobs.delete(id)
          resp <- Ok()
        } yield resp
        case None => NotFound(FailureResponse(s"Cannot delete job $id not found"))
      }
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (all <+> find <+> create <+> update <+> delete)
  )

object JobRoutes:
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]) = new JobRoutes[F](jobs)
