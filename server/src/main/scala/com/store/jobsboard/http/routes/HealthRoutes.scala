package com.store.jobsboard.http.routes

import cats.Monad
import cats.data.Kleisli
import cats.effect.IO
import com.store.foundations.Http4s.{courseRoutes, healthEndpoint}
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.*
import org.http4s.server.Router

class HealthRoutes[F[_]: Monad] private extends Http4sDsl[F]:
  private val healthRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok("All going good.")
  }

  val routes: HttpRoutes[F] = Router(
    "/health" -> healthRoute
  )

object HealthRoutes:
  def apply[F[_]: Monad] = new HealthRoutes[F]
