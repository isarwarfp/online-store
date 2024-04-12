package com.store.jobsboard.core

import cats.data.OptionT
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.store.jobsboard.domain.security.Authenticator
import com.store.jobsboard.domain.user.{NewUserInfo, User}
import com.store.jobsboard.fixtures.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256

import scala.concurrent.duration.given
import scala.language.postfixOps

class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UserFixture:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  private val mockedUser = new Users[IO]:
    override def find(email: String): IO[Option[User]] =
      if(email == imranEmail) IO.pure(Some(IMRAN_ADMIN))
      else IO.pure(None)
    override def create(user: User): IO[String] = IO.pure(user.email)
    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean] = IO.pure(true)

  private val mockedAuthenticator: Authenticator[IO] = {
    // Key for Hashing
    val key = HMACSHA256.unsafeGenerateKey
    // identity store to fetch users
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if(email == imranEmail) OptionT.pure(IMRAN_ADMIN)
      else if(email == imranRecruiterEmail) OptionT.pure(IMRAN_RECRUITER)
      else OptionT.none[IO, User]
    // jwt authenticator
    JWTAuthenticator.unbacked.inBearerToken(
      1 day, None, idStore, key
    )
  }


  "Auth 'algebra'" - {
    "login should return None, if user do not exist" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUser, mockedAuthenticator)
        maybeToken <- auth.login("donot@gmail.com", "pwd")
      } yield maybeToken
      program.asserting(_ shouldBe None)
    }

    "login should return None, if user password is wrong" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUser, mockedAuthenticator)
        maybeToken <- auth.login(imranEmail, "pwd2")
      } yield maybeToken
      program.asserting(_ shouldBe None)
    }

    "login should return a token, if user user credentials are correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUser, mockedAuthenticator)
        maybeToken <- auth.login(imranEmail, "pwd")
      } yield maybeToken
      program.asserting(_ shouldBe defined)
    }

    "signup should not create user after an existing user" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUser, mockedAuthenticator)
        maybeToken <- auth.signUp(NewUserInfo(
          imranEmail,
          "other-pwd",
          Some("Imran"),
          Some("Sarwar"),
          Some("IMG")
        ))
      } yield maybeToken
      program.asserting(_ shouldBe None)
    }

    "signup should create user new user" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUser, mockedAuthenticator)
        maybeToken <- auth.signUp(NewUserInfo(
          "i@k.com",
          "pwd",
          Some("Imran"),
          Some("Sarwar"),
          Some("IMG")
        ))
      } yield maybeToken
      program.asserting {
        case Some(user) =>
          user.email shouldBe "i@k.com"
          user.firstName shouldBe Some("Imran")
        case None => fail()
      }
    }
  }
