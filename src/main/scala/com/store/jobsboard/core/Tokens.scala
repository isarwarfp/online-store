package com.store.jobsboard.core

import doobie.util.transactor.Transactor
import doobie.implicits.*
import com.store.jobsboard.config.TokenConfig
import org.typelevel.log4cats.Logger
import cats.implicits.*
import cats.effect.*
import com.store.jobsboard.core.Users
import com.store.jobsboard.domain.user.User
import scala.util.Random

trait Tokens[F[_]]:
    def getToken(email: String): F[Option[String]]
    def checkToken(email: String, token: String): F[Boolean]

class LiveTokens[F[_]: MonadCancelThrow: Logger](users: Users[F])(xa: Transactor[F], tokenConfig: TokenConfig) extends Tokens[F]:
    override def getToken(email: String): F[Option[String]] = 
        users.find(email).flatMap {
            case None => None.pure[F]
            case Some(user) => getFreshToken(user).map(Some(_))
        }
    override def checkToken(email: String, token: String): F[Boolean] = 
        sql"""
            SELECT token FROM recovery_tokens WHERE email = $email AND token = $token AND expiration > ${System.currentTimeMillis()}
        """
        .query[String]
        .option
        .transact(xa)
        .map(_.nonEmpty)
    
    private def getFreshToken(user: User): F[String] = 
        findToken(user.email).flatMap {
            case None => createToken(user)
            case Some(token) => updateToken(user)
        }
    private def createToken(user: User): F[String] = 
        for {
            token <- randomToken(8)
            _ <- sql"""
                INSERT INTO recovery_tokens (email, token, expiration) 
                VALUES (${user.email}, $token, ${System.currentTimeMillis() + tokenConfig.tokenDuration})
                """
                .update.run.transact(xa)
            } yield token

    private def updateToken(user: User): F[String] = 
        for {
            token <- randomToken(8)
            _ <- sql"""
                UPDATE recovery_tokens 
                SET token = $token, expiration = ${System.currentTimeMillis() + tokenConfig.tokenDuration}
                WHERE email = ${user.email}
                """
                .update.run.transact(xa)
        } yield token

    private def randomToken(maxLength: Int): F[String] = 
        Random.alphanumeric.take(maxLength).mkString.pure[F]

    private def findToken(email: String): F[Option[String]] = 
        sql"""
            SELECT token FROM recovery_tokens WHERE email = $email AND expiration > ${System.currentTimeMillis()}
            """
            .query[String]
            .option
            .transact(xa)

object LiveTokens:
    def apply[F[_]: MonadCancelThrow: Logger](users: Users[F])(xa: Transactor[F], tokenConfig: TokenConfig): F[LiveTokens[F]] =
        new LiveTokens[F](users)(xa, tokenConfig).pure[F]