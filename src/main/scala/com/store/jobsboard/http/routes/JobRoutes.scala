package com.store.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.effect.*
import cats.implicits.*
import cats.data.Kleisli
import cats.effect.IO
import com.store.foundations.Http4s.{courseRoutes, healthEndpoint}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.*
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Request, Response}

import scala.collection.mutable
import com.store.jobsboard.domain.job.*
import com.store.jobsboard.http.responses.FailureResponse

import java.util.UUID

class JobRoutes[F[_]: Concurrent] private extends Http4sDsl[F]:

  // database
  private val database = mutable.Map[UUID, Job]()

  // POST /jobs?offset=x&limit=y { filters } // TODO add query params and filter
  private val all: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root =>
      Ok(database.values)
  }

  // GET /jobs/uuid
  private val find: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
      database.get(id) match
        case Some(job) => Ok(job)
        case None => NotFound(FailureResponse(s"Job with $id not found"))
  }

  private def createJob(jobInfo: JobInfo): F[Job] =
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "i@g.com",
      jobInfo = jobInfo,
      active = true
    ).pure[F]
  // POST /jobs/create { jobsInfo }
  private val create: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        jobInfo <- req.as[JobInfo]
        id      <- createJob(jobInfo)
        resp    <- Created(id)
      } yield resp
  }

  // PUT /jobs/uuid { jobInfo }
  private val update: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      database.get(id) match
        case Some(job) => for {
          jobInfo <- req.as[JobInfo]
          _ <- database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
          resp <- Ok()
        } yield resp
        case None => NotFound(FailureResponse(s"Cannot update job $id not found"))
  }

  // DELETE /jobs/uuid
  private val delete: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) =>
      database.get(id) match
        case Some(_) => for {
          _ <- database.remove(id).pure[F]
          resp <- Ok()
        } yield resp
        case None => NotFound(FailureResponse(s"Cannot delete job $id not found"))
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (all <+> find <+> create <+> update <+> delete)
  )

object JobRoutes:
  def apply[F[_]: Concurrent] = new JobRoutes[F]
