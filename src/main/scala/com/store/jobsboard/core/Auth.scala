package com.store.jobsboard.core

import cats.effect.*
import cats.implicits.*
import com.store.jobsboard.domain.auth.NewPasswordInfo
import com.store.jobsboard.domain.security.{Authenticator, JwtToken}
import com.store.jobsboard.domain.user.*
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

trait Auth[F[_]]:
  // For test consider `Imran` is already in Database
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]]
  def authenticator: Authenticator[F]

class LiveAuth[F[_]: Async: Logger] private (
  users: Users[F],
  override val authenticator: Authenticator[F]
) extends Auth[F]:
  override def login(email: String, password: String): F[Option[JwtToken]] =
    for {
      maybeUser <- users.find(email)
      // Option[User].filter(User => IO[Boolean]) => IO[Option[User]], that's why using filterA
      maybeValidatedUser <- maybeUser.filterA(user =>
        BCrypt.checkpwBool[F](password, PasswordHash[BCrypt](user.hashedPassword))
      )
      // Using map Found: Option[User].map(User => F[JWTToken]) => Option[F[JWTToken]]
      // So to achieve F[Option[JWTToken]] using traverse
      maybeJwtToken <- maybeValidatedUser.traverse(user => authenticator.create(user.email))
    } yield maybeJwtToken
  override def signUp(newUserInfo: NewUserInfo): F[Option[User]] =
    users.find(newUserInfo.email).flatMap {
      case Some(_) => None.pure[F]
      case None =>
        for {
          hashedPassword <- BCrypt.hashpw[F](newUserInfo.password)
          user <- User(
            newUserInfo.email,
            hashedPassword = hashedPassword,
            firstName = newUserInfo.firstName,
            lastName = newUserInfo.lastName,
            company = newUserInfo.company,
            role = Role.RECRUITER
          ).pure[F]
          _ <- users.create(user)
        } yield Some(user)
    }

  override def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]] =
    def updateUser(user: User, newPwd: String): F[Option[User]] = for {
      hashedPwd <- BCrypt.hashpw[F](newPwd)
      updatedUser <- users.update(user.copy(hashedPassword = hashedPwd))
    } yield updatedUser

    def checkAndUpdateUser(user: User, oldPwd: String, newPwd: String): F[Either[String, Option[User]]] = for {
      passCheck <- BCrypt.checkpwBool[F](oldPwd, PasswordHash[BCrypt](user.hashedPassword))
      updatedResult <-
        if(passCheck) updateUser(user, newPwd).map(Right(_))
        else Left("Invalid password").pure[F]
    } yield updatedResult

    users.find(email).flatMap {
      case None => Right(None).pure[F]
      case Some(user) => checkAndUpdateUser(user, newPasswordInfo.oldPassword, newPasswordInfo.newPassword)
    }

object LiveAuth:
  def apply[F[_]: Async: Logger](users: Users[F], authenticator: Authenticator[F]): F[LiveAuth[F]] =
    new LiveAuth[F](users, authenticator).pure[F]

