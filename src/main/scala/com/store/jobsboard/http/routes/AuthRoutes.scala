package com.store.jobsboard.http.routes

import cats.*
import cats.effect.*
import cats.implicits.*
import cats.effect.kernel.Concurrent
import com.store.jobsboard.core.*
import com.store.jobsboard.http.validation.syntax.*
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

class AuthRoutes[F[_]: Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F]:
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "login" => Ok("TODO")
  }
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "users" => Ok("TODO")
  }
  private val changePasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case PUT -> Root / "users" / "password" => Ok("TODO")
  }
  private val logoutRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "logout" => Ok("TODO")
  }

  val routes: HttpRoutes[F] = Router( "/auth" -> (loginRoute <+> createUserRoute <+> changePasswordRoute <+> loginRoute))

object AuthRoutes:
  def apply[F[_]: Concurrent: Logger](auth: Auth[F]): AuthRoutes[F] = new AuthRoutes[F](auth)