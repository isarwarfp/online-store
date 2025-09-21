package com.store.jobsboard.core

import cats.data.OptionT
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.store.jobsboard.core.*
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
import com.store.jobsboard.config.SecurityConfig
import cats.effect.kernel.Ref

class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UserFixture:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  private val mockedSecurityConfig = SecurityConfig("secret", 1.day)
  private val mockedTokens: Tokens[IO] = new Tokens[IO]:
    override def getToken(email: String): IO[Option[String]] = 
      if(email == imranEmail) IO.pure(Some("abc123"))
      else IO.pure(None)
    override def checkToken(email: String, token: String): IO[Boolean] = 
      IO.pure(token == "abc123")
  private val mockedEmails: Emails[IO] = new Emails[IO]:
    override def send(to: String, subject: String, content: String): IO[Unit] = IO.unit
    override def sendRecoveryToken(to: String, token: String): IO[Unit] = IO.unit

  private def probedEmails(users: Ref[IO, Set[String]]): Emails[IO] = new Emails[IO]:
    override def send(to: String, subject: String, content: String): IO[Unit] = users.modify(set => (set + to, ()))
    override def sendRecoveryToken(to: String, token: String): IO[Unit] = send(to, "Recovery Token", s"token")

  // private val mockedAuthenticator: Authenticator[IO] = {
  //   // Key for Hashing
  //   val key = HMACSHA256.unsafeGenerateKey
  //   // identity store to fetch users
  //   val idStore: IdentityStore[IO, String, User] = (email: String) =>
  //     if(email == imranEmail) OptionT.pure(IMRAN_ADMIN)
  //     else if(email == imranRecruiterEmail) OptionT.pure(IMRAN_RECRUITER)
  //     else OptionT.none[IO, User]
  //   // jwt authenticator
  //   JWTAuthenticator.unbacked.inBearerToken(
  //     1 day, None, idStore, key
  //   )
  // }

  "Auth 'algebra'" - {
    "login should return None, if user do not exist" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUser, mockedTokens, mockedEmails)
        maybeToken <- auth.login("donot@gmail.com", "pwd")
      } yield maybeToken
      program.asserting(_ shouldBe None)
    }

    "login should return None, if user password is wrong" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUser, mockedTokens, mockedEmails)
        maybeToken <- auth.login(imranEmail, "pwd2")
      } yield maybeToken
      program.asserting(_ shouldBe None)
    }

    "login should return a token, if user user credentials are correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUser, mockedTokens, mockedEmails)
        maybeToken <- auth.login(imranEmail, "pwd")
      } yield maybeToken
      program.asserting(_ shouldBe defined)
    }

    "signup should not create user after an existing user" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUser, mockedTokens, mockedEmails)
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
        auth <- LiveAuth[IO](mockedUser, mockedTokens, mockedEmails)
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

  "recover password should fail if user does not exist, even if token is valid" in {
    val program = for {
      auth <- LiveAuth[IO](mockedUser, mockedTokens, mockedEmails)
      result <- auth.recoverPasswordFromToken("notfound@gmail.com", "abc123", "newPassword")
    } yield result
    program.asserting(_ shouldBe false)
  }
  
  "recover password should fail if token is invalid, even if user exists" in {
    val program = for {
      auth <- LiveAuth[IO](mockedUser, mockedTokens, mockedEmails)
      result <- auth.recoverPasswordFromToken(imranEmail, "wrongToken", "newPassword")
    } yield result
    program.asserting(_ shouldBe false)
  }

  "recover password should succeed if user and token are valid" in {
    val program = for {
      auth <- LiveAuth[IO](mockedUser, mockedTokens, mockedEmails)
      result <- auth.recoverPasswordFromToken(imranEmail, "abc123", "newPassword")
    } yield result
    program.asserting(_ shouldBe true)
  }

  "recovery sending email should be failed if user does not exist" in {
    val program = for {
      set <- Ref.of[IO, Set[String]](Set.empty)
      emails = probedEmails(set)
      auth <- LiveAuth[IO](mockedUser, mockedTokens, emails)
      result <- auth.sendRecoveryToken("notfound@gmail.com")
      emails <- set.get
    } yield emails
    program.asserting(_ shouldBe Set.empty)
  }

  "recovery sending email should be succeeded if user exists" in {
    val program = for {
      set <- Ref.of[IO, Set[String]](Set.empty)
      emails = probedEmails(set)
      auth <- LiveAuth[IO](mockedUser, mockedTokens, emails)
      result <- auth.sendRecoveryToken(imranEmail)
      emails <- set.get
    } yield emails
    program.asserting(_.shouldBe(Set(imranEmail)))
  } 
  
