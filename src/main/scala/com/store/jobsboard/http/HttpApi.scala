package com.store.jobsboard.http

import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.*
import org.http4s.server.Router
import cats.*
import cats.effect.*
import cats.implicits.*
import com.store.jobsboard.http.routes.{HealthRoutes, JobRoutes}
import org.http4s.HttpRoutes

class HttpApi[F[_]: Concurrent] private:
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes = JobRoutes[F].routes

  val endpoints: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )

object HttpApi:
  def apply[F[_]: Concurrent] = new HttpApi[F]
