package com.store.jobsboard.domain

import com.store.jobsboard.domain.user.Role.{ADMIN, RECRUITER}
import doobie.util.meta.Meta
import tsec.authorization.{AuthGroup, SimpleAuthEnum}
import com.store.jobsboard.domain.job.Job

object user:

  final case class User(
    email: String,
    hashedPassword: String,
    firstName: Option[String],
    lastName: Option[String],
    company: Option[String],
    role: Role
  ) {
    def owns(job: Job): Boolean = email == job.ownerEmail
    def isAdmin: Boolean = role == ADMIN
    def isRecruiter: Boolean = role == RECRUITER
  }

  final case class NewUserInfo(
    email: String,
    password: String,
    firstName: Option[String],
    lastName: Option[String],
    company: Option[String]
  )

  enum Role:
    case ADMIN, RECRUITER

  object Role:
    given metaRole: Meta[Role] =
      Meta[String].timap[Role](Role.valueOf)(_.toString)

  // Because of Scala 3, we need to define a SimpleAuthEnum for the 
  // Role enum to convert it to a String
  given roleAuthEnum: SimpleAuthEnum[Role, String] with
    override val values: AuthGroup[Role] = AuthGroup(ADMIN, RECRUITER)
    override def getRepr(role: Role): String = role.toString
