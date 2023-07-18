package com.store.jobsboard.http.routes

import cats.*
import cats.implicits.*
import cats.data.Kleisli
import cats.effect.IO
import com.store.foundations.Http4s.{courseRoutes, healthEndpoint}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.*
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Request, Response}

class JobRoutes[F[_]: Monad] private extends Http4sDsl[F]:

  // POST /jobs?offset=x&limit=y { filters } // TODO add query params and filter
  private val all: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root =>
      Ok("TODO")
  }

  // GET /jobs/uuid
  private val find: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
      Ok(s"TODO find job for $id")
  }

  // POST /jobs/create { jobsInfo }
  private val create: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "create" =>
      Ok("TODO")
  }

  // PUT /jobs/uuid { jobInfo }
  private val update: HttpRoutes[F] = HttpRoutes.of[F] {
    case PUT -> Root / UUIDVar(id) =>
      Ok(s"TODO update job for $id")
  }

  // DELETE /jobs/uuid
  private val delete: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) =>
      Ok(s"TODO delete job for $id")
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (all <+> find <+> create <+> update <+> delete)
  )

object JobRoutes:
  def apply[F[_]: Monad] = new JobRoutes[F]
