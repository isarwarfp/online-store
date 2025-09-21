package com.store.jobsboard.domain

object auth:
  final case class LoginInfo(
    email: String,
    password: String
  )

  final case class NewPasswordInfo(
    oldPassword: String,
    newPassword: String
  )

  final case class ForgotPasswordInfo(
    email: String
  )

  final case class ResetPasswordInfo(
    email: String,
    token: String,
    newPassword: String
  )