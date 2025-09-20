package com.store.jobsboard.core

import cats.effect.*
import cats.implicits.*
import com.store.jobsboard.domain.auth.NewPasswordInfo
import com.store.jobsboard.domain.security.{Authenticator, JwtToken}
import com.store.jobsboard.domain.user.*
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt
import tsec.authentication.JWTAuthenticator
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import cats.data.OptionT
import tsec.authentication.BackingStore
import tsec.common.SecureRandomId

import scala.concurrent.duration.given
import com.store.jobsboard.config.SecurityConfig

trait Auth[F[_]]:
  // For test consider `Imran` is already in Database
  def login(email: String, password: String): F[Option[User]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]]
  // We are removing authenticator from here
  def delete(email: String): F[Boolean]
  def sendRecoveryToken(email: String): F[Unit]
  def recoverPasswordFromToken(email: String, token: String, newPassword: String): F[Boolean]

class LiveAuth[F[_]: Async: Logger] private (users: Users[F], tokens: Tokens[F], emails: Emails[F]) extends Auth[F]:
  override def login(email: String, password: String): F[Option[User]] =
    for {
      maybeUser <- users.find(email)
      // Option[User].filter(User => IO[Boolean]) => IO[Option[User]], that's why using filterA
      maybeValidatedUser <- maybeUser.filterA(user =>
        BCrypt.checkpwBool[F](password, PasswordHash[BCrypt](user.hashedPassword))
      )
    } yield maybeValidatedUser
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

  override def delete(email: String): F[Boolean] =
    users.delete(email)
  
  override def sendRecoveryToken(email: String): F[Unit] = 
    tokens.getToken(email).flatMap {
      case None => ().pure[F]
      case Some(token) => emails.sendRecoveryToken(email, token)
    }
  override def recoverPasswordFromToken(email: String, token: String, newPassword: String): F[Boolean] = 
    for {
      maybeUser <- users.find(email)
      tokenIsValid <- tokens.checkToken(email, token)
      result <- (maybeUser, tokenIsValid) match {
        case (Some(user), true) => updateUser(user, newPassword).map(_.nonEmpty)
        case _ => false.pure[F]
      }
    } yield result

  private def updateUser(user: User, newPwd: String): F[Option[User]] = for {
    hashedPwd <- BCrypt.hashpw[F](newPwd)
    updatedUser <- users.update(user.copy(hashedPassword = hashedPwd))
  } yield updatedUser

object LiveAuth:
  // Removed from apply argument
  // Authenticator is to be removed from here as it should be part of web layer
  def apply[F[_]: Async: Logger](users: Users[F], tokens: Tokens[F], emails: Emails[F]): F[LiveAuth[F]] =
    new LiveAuth[F](users, tokens, emails).pure[F]

