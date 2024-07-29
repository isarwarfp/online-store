package com.store.jobsboard.domain

import com.store.jobsboard.domain.user.Role.{ADMIN, RECRUITER}
import doobie.util.meta.Meta
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

object user:

  final case class User(
    email: String,
    hashedPassword: String,
    firstName: Option[String],
    lastName: Option[String],
    company: Option[String],
    role: Role
  )

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

  given roleAuthEnum: SimpleAuthEnum[Role, String] with
    override val values: AuthGroup[Role] = AuthGroup(ADMIN, RECRUITER)
    override def getRepr(role: Role): String = role.toString
