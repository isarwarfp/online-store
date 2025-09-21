package com.store.jobsboard.domain

import cats.*
import cats.implicits.*
import org.http4s.{Response, Status}
import com.store.jobsboard.domain.user.*
import tsec.authentication.{AugmentedJWT, JWTAuthenticator, SecuredRequest, TSecAuthService}
import tsec.mac.jca.HMACSHA256
import tsec.authorization.{AuthorizationInfo, BasicRBAC}
import tsec.authorization.BasicRBAC.*
import tsec.authentication.SecuredRequestHandler

object security:
  type Crypto = HMACSHA256
  type JwtToken = AugmentedJWT[Crypto, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, Crypto]
  type AuthRoute[F[_]] = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
  // RBAC: Role Base Access Control: That takes JwtToken and finds the role of the user
  type AuthRBAC[F[_]] = BasicRBAC[F, Role, User, JwtToken]
  type SecuredHandler[F[_]] = SecuredRequestHandler[F, String, User, JwtToken]
  object SecuredHandler:
    def apply[F[_]](using handler: SecuredHandler[F]): SecuredHandler[F] = handler

  // RBAC: Role Base Access Control
  given authRole[F[_]: Applicative]: AuthorizationInfo[F, Role, User] with
    override def fetchInfo(u: User): F[Role] = u.role.pure[F]

  def allRoles[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC.all[F, Role, User, JwtToken]

  def recruiterOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.RECRUITER)

  def onlyAdmin[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.ADMIN)

  case class Authorizations[F[_]](rbacRoutes: Map[AuthRBAC[F], List[AuthRoute[F]]])
  object Authorizations:
    // Step 3. Semigroup for authorization
    given combiner[F[_]]: Semigroup[Authorizations[F]] = Semigroup.instance { (authA, authB) =>
      Authorizations(authA.rbacRoutes |+| authB.rbacRoutes)
    }
  // Expected: AuthRoute -> Authorizations -> TSecAuthService -> HttpRoute
  // Step 1. AuthRoute -> Authorizations = extension restrictedTo
  extension [F[_]] (authRoute: AuthRoute[F])
    def restrictedTo(rbac: AuthRBAC[F]): Authorizations[F] =
      Authorizations(Map(rbac -> List(authRoute)))

  // Step 2. Authorization -> TSecAuthService = implicit conversion
  given auth2tsec[F[_]: Monad]: Conversion[Authorizations[F], TSecAuthService[User, JwtToken, F]] =
    authz => {
      // it responds 401 always
      val unAuthService: TSecAuthService[User, JwtToken, F] =
        TSecAuthService[User, JwtToken, F] { _ =>
          Response[F](Status.Unauthorized).pure[F]
        }

      authz.rbacRoutes
        .toSeq
        .foldLeft(unAuthService) {
          case (acc, (rbac, routes)) =>
            val bigRoutes = routes.reduce(_.orElse(_))
            // Build new service, fall back to acc if rbac/routes fails
            TSecAuthService.withAuthorizationHandler(rbac)(bigRoutes, acc.run)
        }
    }