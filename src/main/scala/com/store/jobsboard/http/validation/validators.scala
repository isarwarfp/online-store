package com.store.jobsboard.http.validation

import cats.*
import cats.implicits.*
import cats.data.*
import cats.data.ValidatedNel
import com.store.jobsboard.domain.job.*
import scala.util.*
import com.store.jobsboard.http.validation.validators.ValidationFailed.*
import java.net.URL

object validators:
  sealed trait ValidationFailed(val message: String)
  object ValidationFailed {
    final case class EmptyField(fieldName: String) extends ValidationFailed(s"'$fieldName' is empty")
    final case class InvalidURL(fieldName: String) extends ValidationFailed(s"'$fieldName' is invalid URL")
  }

  type ValidationResult[A] = ValidatedNel[ValidationFailed, A]

  trait Validator[A]:
    def validateAs(value: A): ValidationResult[A]

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