package com.store.jobsboard.core

import cats.*
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.postgres.implicits.*
import org.typelevel.log4cats.Logger
import com.store.jobsboard.logging.syntax.*

import com.store.jobsboard.domain.user.User

trait Users[F[_]]:
  def find(email: String): F[Option[User]]
  def create(user: User): F[String]
  def update(user: User): F[Option[User]]
  def delete(email: String): F[Boolean]

final class LiveUser[F[_]: MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Users[F]:
  override def find(email: String): F[Option[User]] =
    sql"SELECT * FROM users WHERE email=${email}"
      .query[User]
      .option
      .transact(xa)

  override def create(user: User): F[String] =
    sql"""
      INSERT INTO users (
        email,
        hashedPassword,
        firstName,
        lastName,
        company,
        role
      ) VALUES (
        ${user.email},
        ${user.hashedPassword},
        ${user.firstName},
        ${user.lastName},
        ${user.company},
        ${user.role}
      )
      """
      .update
      .run
      .transact(xa)
      .map(_ => user.email)

  override def update(user: User): F[Option[User]] =
    for {
      _ <- sql"""
           UPDATE users SET
            hashedPassword = ${user.hashedPassword},
            firstName = ${user.firstName},
            lastName = ${user.lastName},
            company = ${user.company},
            role = ${user.role}
           WHERE email = ${user.email}
         """.update.run.transact(xa)
      mayBeUser <- find(user.email)
    } yield mayBeUser

  override def delete(email: String): F[Boolean] =
    sql"DELETE FROM users WHERE email = $email"
      .update.run.transact(xa)
      .map(_ > 0)

object LiveUser:
  def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveUser[F]] =
    new LiveUser[F](xa).pure[F]