package com.store.jobsboard.modules

import cats.*
import cats.effect.*
import cats.implicits.*
import com.store.jobsboard.http.routes.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F]):
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes = JobRoutes[F](core.jobs).routes
  private val authRoutes = AuthRoutes[F](core.auth).routes

  val endpoints: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> jobRoutes <+> authRoutes)
  )

object HttpApi:
  def apply[F[_]: Concurrent: Logger](core: Core[F]): Resource[F, HttpApi[F]] =
    Resource.pure(new HttpApi[F](core))
