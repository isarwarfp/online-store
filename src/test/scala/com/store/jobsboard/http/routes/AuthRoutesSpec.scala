package com.store.jobsboard.http.routes

import cats.data.OptionT
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.store.jobsboard.core.*
import com.store.jobsboard.domain.auth.*
import com.store.jobsboard.domain.pagination.*
import com.store.jobsboard.domain.job.*
import com.store.jobsboard.domain.security.{Authenticator, JwtToken}
import com.store.jobsboard.domain.user.{NewUserInfo, User}
import com.store.jobsboard.fixtures.{JobFixture, UserFixture}
import com.store.jobsboard.fixtures.SecuredRouteFixture
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.scalatest.freespec.{AnyFreeSpec, AsyncFreeSpec}
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256
import scala.concurrent.duration.given

import scala.language.postfixOps
import tsec.authentication.SecuredRequestHandler

class AuthRoutesSpec extends AsyncFreeSpec with AsyncIOSpec with Http4sDsl[IO] with Matchers with SecuredRouteFixture:
  val mockedAuth: Auth[IO] = new Auth[IO] {
    override def login(email: String, password: String): IO[Option[User]] =
      if( email == imranEmail && password == imranPassword)
        Some(IMRAN_ADMIN).pure[IO]
      else IO.pure(None)
    override def signUp(newUserInfo: NewUserInfo): IO[Option[User]] =
      if(newUserInfo.email == newUserEmail)
        IO.pure(Some(NEW_USER))
      else IO.pure(None)
    override def changePassword(email: String, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] =
      if(email == imranEmail)
        if(newPasswordInfo.oldPassword == imranPassword)
          IO.pure(Right(Some(IMRAN_ADMIN)))
        else IO.pure(Left("Invalid password"))
      else IO.pure(Right(None))

    override def delete(email: String): IO[Boolean] = IO.pure(true)
  }
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth, mockedAuthenticator).routes

  "AuthRoutes" - {
    "Should return a 401 - unauthorised if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(imranEmail, "wrongPwd"))
        )
      } yield response.status shouldBe Status.Unauthorized
    }

    "should return 200 + JWT token if login successful" in  {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(imranEmail, imranPassword))
        )
      } yield {
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined
      }
    }

    "should return 400 and Bad Request, when user already in DB" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users").withEntity(NEW_USER_IMRAN_ADMIN))
      } yield response.status shouldBe Status.BadRequest
    }

    "should return 201 Created, on creating new user" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users").withEntity(NEW_USER_CREATION))
      } yield response.status shouldBe Status.Created
    }

    "should return 200 - Ok, if logging out with valid JWT Token" in {
      for {
        jwtToken <- mockedAuthenticator.create(imranEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
            .withBearerToken(jwtToken)
        )
      } yield response.status shouldBe Status.Ok
    }

    "should return 401 Unauthorized, if logging out without JWT Token" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      } yield response.status shouldBe Status.Unauthorized
    }

    "should return 401 Unauthorized, if non admin delete user" in {
      for {
        jwtToken <- mockedAuthenticator.create(imranRecruiterEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/i@gmail.com")
            .withBearerToken(jwtToken)
        )
      } yield response.status shouldBe Status.Unauthorized
    }

    "should return 200 Ok, if an admin delete user" in {
      for {
        jwtToken <- mockedAuthenticator.create(imranEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/i2@gmail.com")
            .withBearerToken(jwtToken)
        )
      } yield response.status shouldBe Status.Ok
    }
  }
