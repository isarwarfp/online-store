package com.store.jobsboard.modules

import cats.*
import cats.effect.*
import cats.data.*
import cats.implicits.*
import com.store.jobsboard.http.routes.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt
import tsec.authentication.JWTAuthenticator
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import cats.data.OptionT
import tsec.authentication.BackingStore
import tsec.common.SecureRandomId

import com.store.jobsboard.domain.security.*
import com.store.jobsboard.domain.user.*
import com.store.jobsboard.config.*
import com.store.jobsboard.core.Users
import tsec.authentication.SecuredRequestHandler

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F], authenticator: Authenticator[F]):
  given securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes = JobRoutes[F](core.jobs).routes
  private val authRoutes = AuthRoutes[F](core.auth, authenticator).routes

  val endpoints: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> jobRoutes <+> authRoutes)
  )

object HttpApi:
  def createAuthenticator[F[_]: Sync](users: Users[F], securityConfig: SecurityConfig): F[Authenticator[F]] = 
    // 1. Identity Store | String => OptionT[F, User]
    val idStore: IdentityStore[F, String, User] = (email: String) => OptionT(users.find(email))

    // 2. Backing store for JWT Token | BackingStore[F, id, JwtToken]
    // BackingStore is like a map of elements with key as id and value as JwtToken
    // Ref is a mutable reference that can be used to store a value in a thread-safe way
    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken]:
        override def get(id: SecureRandomId): OptionT[F, JwtToken] = 
          OptionT(ref.get.map(_.get(id)))
        override def put(elem: JwtToken): F[JwtToken] = 
          ref.modify(store => (store + (elem.id -> elem), elem))
        override def update(v: JwtToken): F[JwtToken] = 
          put(v)
        override def delete(id: SecureRandomId): F[Unit] = 
          ref.modify(store => (store - id, ()))
    }
    
    // 3. Key for Hashing
    val keyF = HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8"))

    // backed: require a backing store
    for {
      tokenStore <- tokenStoreF
      key        <- keyF
      // 4. Authenticator
    } yield JWTAuthenticator.backed.inBearerToken(
          expiryDuration = securityConfig.jwtExpiryDuration, // expiry duration of the token
          maxIdle = None, // max idle time of the token
          identityStore = idStore, // identity store to fetch users
          tokenStore = tokenStore, // token store to store the token
          signingKey = key // key for signing the token
        ) 

  def apply[F[_]: Async: Logger](core: Core[F], securityConfig: SecurityConfig): Resource[F, HttpApi[F]] =
    Resource
    .eval(createAuthenticator(core.users, securityConfig))
    .map(authenticator => new HttpApi[F](core, authenticator))
