package com.store.jobsboard.http.validation

import cats.*
import cats.implicits.*
import cats.data.*
import cats.data.ValidatedNel
import scala.util.*
import com.store.jobsboard.http.validation.validators.ValidationFailed.*
import java.net.URL
import com.store.jobsboard.domain.job.*
import com.store.jobsboard.domain.user.*
import com.store.jobsboard.domain.auth.*

object validators:
  sealed trait ValidationFailed(val message: String)
  object ValidationFailed:
    final case class EmptyField(fieldName: String) extends ValidationFailed(s"'$fieldName' is empty")
    final case class InvalidURL(fieldName: String) extends ValidationFailed(s"'$fieldName' is invalid URL")
    final case class InvalidEmail(fieldName: String) extends ValidationFailed(s"'$fieldName' is invalid email")

  type ValidationResult[A] = ValidatedNel[ValidationFailed, A]

  trait Validator[A]:
    def validateAs(value: A): ValidationResult[A]

  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  def validateEmail(field: String, fieldName: String): ValidationResult[String] =
    if(emailRegex.findFirstMatchIn(field).isDefined) field.validNel
    else InvalidEmail(fieldName).invalidNel

  def validateRequired[A](value: A, fieldName: String)(require: A => Boolean): ValidationResult[A] =
    if(require(value)) value.validNel
    else EmptyField(fieldName).invalidNel

  def validateURL(field: String, fieldName: String): ValidationResult[String] =
    Try(URL(field).toURI) match {
      case Success(_) => field.validNel
      case Failure(e) => InvalidURL(fieldName).invalidNel
    }

  given jobInfoValidator: Validator[JobInfo] = (jobInfo: JobInfo) => {
    val JobInfo(
      company, // Should not be Empty
      title, // Should not be Empty
      description, // Should not be Empty
      externalUrl, // Should be valid URL
      remote,
      location, // Should not be Empty
      salaryLo,
      salaryHi,
      currency,
      country,
      tags,
      image,
      seniority,
      other
    ) = jobInfo

    val validCompany = validateRequired(company, "company")(_.nonEmpty)
    val validTitle = validateRequired(title, "title")(_.nonEmpty)
    val validDesc = validateRequired(description, "description")(_.nonEmpty)
    val validExternalUrl = validateURL(externalUrl, "externalUrl")
    val validLocation = validateRequired(location, "location")(_.nonEmpty)

    (
      validCompany,
      validTitle,
      validDesc,
      validExternalUrl,
      remote.validNel,
      validLocation,
      salaryLo.validNel,
      salaryHi.validNel,
      currency.validNel,
      country.validNel,
      tags.validNel,
      image.validNel,
      seniority.validNel,
      other.validNel
    ).mapN(JobInfo.apply)
  }

  given loginInfoValidator: Validator[LoginInfo] = (loginInfo: LoginInfo) => {
    val validUserEmail = validateRequired(loginInfo.email, "email")(_.nonEmpty)
      .andThen(e => validateEmail(e, "email"))
    val validUserPassword = validateRequired(loginInfo.password, "password")(_.nonEmpty)
    (validUserEmail, validUserPassword).mapN(LoginInfo.apply)
  }

  given newUserInfoValidator: Validator[NewUserInfo] = (newUserInfo: NewUserInfo) => {
    val validUserEmail = validateRequired(newUserInfo.email, "email")(_.nonEmpty)
      .andThen(e => validateEmail(e, "email"))
    val validUserPassword = validateRequired(newUserInfo.password, "password")(_.nonEmpty)
    (
      validUserEmail,
      validUserPassword,
      newUserInfo.firstName.validNel,
      newUserInfo.lastName.validNel,
      newUserInfo.company.validNel
    ).mapN(NewUserInfo.apply)
  }

  given newPasswordValidator: Validator[NewPasswordInfo] = (newPasswordInfo: NewPasswordInfo) => {
    val validOldPassword = validateRequired(newPasswordInfo.oldPassword, "old password")(_.nonEmpty)
    val validNewPassword = validateRequired(newPasswordInfo.newPassword, "new password")(_.nonEmpty)
    (
      validOldPassword,
      validNewPassword
    ).mapN(NewPasswordInfo.apply)
  }