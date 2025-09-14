package com.store.jobsboard.fixtures

import cats.data.*
import cats.effect.*
import org.http4s.*
import org.http4s.headers.*
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator
import tsec.mac.jca.HMACSHA256
import tsec.jws.mac.JWTMac

import scala.concurrent.duration.*

import com.store.jobsboard.domain.user.*
import com.store.jobsboard.domain.security.*
import tsec.authentication.SecuredRequestHandler

trait SecuredRouteFixture extends UserFixture {
  val mockedAuthenticator: Authenticator[IO] = {
    // Key for Hashing
    val key = HMACSHA256.unsafeGenerateKey
    // identity store to fetch users
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == imranEmail) OptionT.pure(IMRAN_ADMIN)
      else if (email == imranRecruiterEmail) OptionT.pure(IMRAN_RECRUITER)
      else if (email == newUserEmail) OptionT.pure(NEW_USER)
      else OptionT.none[IO, User]
    // jwt authenticator
    JWTAuthenticator.unbacked.inBearerToken(
      1.day, None, idStore, key
    )
  }
  extension (r: Request[IO])
    def withBearerToken(a: JwtToken): Request[IO] =
      r.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](a.jwt)
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }
    
  given securedHandler: SecuredHandler[IO] = SecuredRequestHandler(mockedAuthenticator)
}
