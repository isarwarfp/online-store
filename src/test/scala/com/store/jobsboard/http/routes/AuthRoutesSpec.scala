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

class AuthRoutesSpec extends AsyncFreeSpec with AsyncIOSpec with Http4sDsl[IO] with Matchers with UserFixture:
  val mockedAuth: Auth[IO] = new Auth[IO] {
    def login(email: String, password: String): IO[Option[JwtToken]] = ???
    def signUp(newUserInfo: NewUserInfo): IO[Option[User]] = ???
    def changePassword(email: String, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] = ???
  }
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth).routes

  extension (r: Request[IO])
    def withBearerToken(a: JwtToken): Request[IO] =
      r.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](a.jwt)
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }

  private val mockedAuthenticator: Authenticator[IO] = {
    // Key for Hashing
    val key = HMACSHA256.unsafeGenerateKey
    // identity store to fetch users
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == imranEmail) OptionT.pure(IMRAN_ADMIN)
      else if (email == imranRecruiterEmail) OptionT.pure(IMRAN_RECRUITER)
      else OptionT.none[IO, User]
    // jwt authenticator
    JWTAuthenticator.unbacked.inBearerToken(
      1 day, None, idStore, key
    )
  }

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
            .withEntity(LoginInfo(imranEmail, "pwd"))
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
      } yield {
        response.status shouldBe Status.Created
        response.headers.get(ci"Authorization") shouldBe defined
      }
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
  }
