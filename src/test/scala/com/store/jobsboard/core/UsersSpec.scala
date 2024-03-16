package com.store.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.store.jobsboard.domain.user.User
import com.store.jobsboard.fixtures.UserFixture

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*

import org.postgresql.util.PSQLException
import org.scalatest.Inside
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class  UsersSpec  extends AsyncFreeSpec with AsyncIOSpec with Matchers with DoobieSpec with Inside with UserFixture:
  override val initScript: String = "sql/users.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Users 'Algebra'" - {
    "should return None when email not found" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUser[IO](xa)
          user <- users.find("notfound@gmail.com")
        } yield user

        program.asserting(_ shouldBe None)
      }
    }

    "should find user successfully with email" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUser[IO](xa)
          user  <- users.find("i@gmail.com")
        } yield user

        program.asserting(_ shouldBe Some(IMRAN_ADMIN))
      }
    }

    "should fail creating new user if email already exist" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUser[IO](xa)
          user <- users.create(IMRAN_ADMIN).attempt
        } yield user

        program.asserting { outcome =>
          inside(outcome) {
            case Left(e) => e shouldBe a[PSQLException]
            case _       => fail()
          }
        }
      }
    }

    "should successfully create new user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUser[IO](xa)
          uId <- users.create(NEW_USER)
          maybeUser <- sql"SELECT * FROM users WHERE email=${NEW_USER.email}"
            .query[User]
            .option
            .transact(xa)
        } yield (uId, maybeUser)

        program.asserting {
          case (userId, maybeUser) =>
            userId shouldBe NEW_USER.email
            maybeUser shouldBe Some(NEW_USER)
        }
      }
    }

    "should return None when updating a user that don't exist" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUser[IO](xa)
          maybeUser <- users.update(NEW_USER)
        } yield maybeUser

        program.asserting(_ shouldBe None)
      }
    }

    "should update existing user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUser[IO](xa)
          maybeUser <- users.update(IMRAN_ADMIN_UPDATED)
        } yield maybeUser

        program.asserting(_ shouldBe Some(IMRAN_ADMIN_UPDATED))
      }
    }

    "should delete successfully" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUser[IO](xa)
          deletedUser <- users.delete("i@gmail.com")
        } yield deletedUser

        program.asserting(_ shouldBe true)
      }
    }

    "should not delete user that doesn't exist" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUser[IO](xa)
          result <- users.delete("i10@gmail.com")
          maybeUser <- sql"SELECT * FROM users WHERE email='i10@gmail.com'"
            .query[User]
            .option
            .transact(xa)
        } yield (result, maybeUser)

        program.asserting {
          case (result, maybeUser) =>
            result shouldBe false
            maybeUser shouldBe None
        }
      }
    }
  }
