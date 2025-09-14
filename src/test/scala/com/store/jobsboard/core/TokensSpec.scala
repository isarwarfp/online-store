package com.store.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.store.jobsboard.core.*
import com.store.jobsboard.fixtures.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.store.jobsboard.config.TokenConfig
import scala.concurrent.duration.DurationInt

class TokensSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with DoobieSpec with UserFixture:
    override val initScript: String = "sql/recovery_tokens.sql"
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    "Tokens 'Algebra'" - {
        "should not create a new token if email does not exist" in {
            transactor.use { xa =>
                val program = for {
                    tokens <- LiveTokens[IO](mockedUser)(xa, TokenConfig(1.day.toMillis))
                    token <- tokens.getToken("notfound@gmail.com")
                } yield token

                program.asserting(_ shouldBe None)
            }
        }

        "should create a new token if email exists" in {
            transactor.use { xa =>
                val program = for {
                    tokens <- LiveTokens[IO](mockedUser)(xa, TokenConfig(1.day.toMillis))
                    token <- tokens.getToken(imranEmail)
                } yield token

                program.asserting(_ shouldBe defined)
            }
        }

        "should check if token expired" in {
            transactor.use { xa =>
                val program = for {
                    tokens <- LiveTokens[IO](mockedUser)(xa, TokenConfig(100L))
                    maybeToken <- tokens.getToken(imranEmail)
                    _ <- IO.sleep(1.second)
                    isTokenExpired <- maybeToken match
                        case Some(token) => tokens.checkToken(imranEmail, token)
                        case None => IO.pure(false)
                } yield isTokenExpired

                program.asserting(_ shouldBe false)
            }
        }

        "should check if token is not expired" in {
            transactor.use { xa =>
                val program = for {
                    tokens <- LiveTokens[IO](mockedUser)(xa, TokenConfig(1.day.toMillis))
                    maybeToken <- tokens.getToken(imranEmail)
                    isTokenExpired <- maybeToken match
                        case Some(token) => tokens.checkToken(imranEmail, token)
                        case None => IO.pure(false)
                } yield isTokenExpired

                program.asserting(_ shouldBe true)
            }
        }

        "should only validate token for the correct email" in {
            transactor.use { xa =>
                val program = for {
                    tokens                  <- LiveTokens[IO](mockedUser)(xa, TokenConfig(1.day.toMillis))
                    maybeImranToken         <- tokens.getToken(imranEmail)
                    isImranTokenValid       <- maybeImranToken match
                        case Some(token) => tokens.checkToken(imranEmail, token)
                        case None        => IO.pure(false)
                    someoneElseToken        <- tokens.getToken("someone@gmail.com")
                    isSomeoneElseTokenValid <- someoneElseToken match
                        case Some(token) => tokens.checkToken("someone@gmail.com", token)
                        case None        => IO.pure(false)
                } yield (isImranTokenValid, isSomeoneElseTokenValid)

                program.asserting(_ shouldBe (true, false))
            }
        }
    }
