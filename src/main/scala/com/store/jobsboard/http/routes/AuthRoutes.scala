package com.store.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.effect.*
import cats.implicits.*
import cats.effect.kernel.Concurrent
import com.store.jobsboard.core.*
import com.store.jobsboard.domain.auth.{LoginInfo, NewPasswordInfo}
import com.store.jobsboard.domain.security.*
import com.store.jobsboard.domain.user.*
import com.store.jobsboard.http.responses.FailureResponse
import com.store.jobsboard.http.validation.syntax.*
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import tsec.authentication.{SecuredRequestHandler, TSecAuthService, asAuthed}
import scala.language.implicitConversions

class AuthRoutes[F[_]: Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F]:
  private val authenticator = auth.authenticator
  private val securedHandler: SecuredRequestHandler[F, String, User, JwtToken] =
    SecuredRequestHandler(authenticator)
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req.validateAs[LoginInfo] { loginInfo =>
        val maybeJwtToken = for {
          maybeToken <- auth.login(loginInfo.email, loginInfo.password)
          _ <- Logger[F].info(s"User Logging In: ${loginInfo.email}")
        } yield maybeToken
        maybeJwtToken.map {
          case Some(token) =>
            println(authenticator)
            authenticator.embed(Response(Status.Ok), token)
          case None => Response(Status.Unauthorized)
        }
      }
  }
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      req.validateAs[NewUserInfo] { newUserInfo =>
        for {
          maybeNewUser <- auth.signUp(newUserInfo)
          resp <- maybeNewUser match {
            case Some(user) => Created(user.email)
            case None => BadRequest(s"User wih email ${newUserInfo.email} already exists.")
          }
        } yield resp
      }
  }
  private val changePasswordRoute: AuthRoute[F] = {
    case req @ PUT -> Root / "users" / "password" asAuthed user =>
      req.request.validateAs[NewPasswordInfo] { newPasswordInfo =>
        for {
          maybeUserOrError <- auth.changePassword(user.email, newPasswordInfo)
          resp <- maybeUserOrError match {
            case Right(Some(_)) => Ok()
            case Right(None) => NotFound(FailureResponse(s"User ${user.email} not found."))
            case Left(_) => Forbidden()
          }
        } yield resp
      }
  }

  private val logoutRoute: AuthRoute[F] = {
    case req @ POST -> Root / "logout" asAuthed _ =>
      val token = req.authenticator
      for {
        _ <- authenticator.discard(token)
        resp <- Ok()
      } yield resp
  }

  private val deleteUserRoute: AuthRoute[F] = {
    case req @ DELETE -> Root / "users" / email asAuthed user =>
      auth.delete(email).flatMap {
        case true => Ok()
        case false => NotFound()
      }
  }

  val unAuthedRoutes = loginRoute <+> createUserRoute
  val authedRoutes = securedHandler.liftService {
    changePasswordRoute.restrictedTo(allRoles) |+|
      logoutRoute.restrictedTo(allRoles) |+|
      deleteUserRoute.restrictedTo(onlyAdmin)
//    TSecAuthService(changePasswordRoute.orElse(logoutRoute).orElse(deleteUserRoute))
  }
  val routes: HttpRoutes[F] = Router( "/auth" -> (unAuthedRoutes <+> authedRoutes))

object AuthRoutes:
  def apply[F[_]: Concurrent: Logger](auth: Auth[F]): AuthRoutes[F] = new AuthRoutes[F](auth)