package com.store.jobsboard.http.validation

import cats.*
import cats.data.Validated.*
import cats.data.*
import cats.implicits.*
import com.store.jobsboard.http.responses.FailureResponse
import com.store.jobsboard.http.validation.validators.*
import com.store.jobsboard.logging.syntax.logError
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceInstances
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.typelevel.log4cats.Logger
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder

object syntax:

  def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validateAs(entity)

  trait HttpValidationDsl[F[_]: MonadThrow: Logger] extends Http4sDsl[F]:
    extension (req: Request[F])
      def validateAs[A: Validator](logicAfterValidation: A => F[Response[F]])
                       (using decoder: EntityDecoder[F,A]): F[Response[F]] =
        req.as[A]
          .logError(e => s"Parsing payload error: $e")
          .map(validateEntity) // F[ValidationResult[F]
          .flatMap {
            case Valid(entity) => logicAfterValidation(entity)
            case Invalid(errs) => BadRequest(FailureResponse(errs.toList.map(_.message).mkString(", ")))
          }
