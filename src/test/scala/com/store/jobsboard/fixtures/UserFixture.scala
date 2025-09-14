package com.store.jobsboard.fixtures

import com.store.jobsboard.domain.user.Role.*
import com.store.jobsboard.domain.user.{NewUserInfo, User}
import com.store.jobsboard.core.Users
import cats.effect.IO

trait UserFixture:
  val imranEmail = "i@gmail.com"
  val imranPassword = "pwd"
  val IMRAN_ADMIN: User = User(
    imranEmail,
    "$2a$10$FqgaSbLZ5MEPQvD5qDTV5e0Xz/N8q3oT027ZwtLDsSIhMoQaMFGjC",
    Some("Imran"),
    Some("Sarwar"),
    Some("IMG"),
    ADMIN
  )

  val imranRecruiterEmail = "i2@gmail.com"
  val IMRAN_RECRUITER: User = User(
    imranRecruiterEmail,
    "$2a$10$OLVVnNYAlUwFmjpSCoYnWeEkkeJBMQcNcKXR01m.gK.01McgpzudO",
    Some("Imran"),
    Some("Sarwar"),
    Some("IMG"),
    RECRUITER
  )

  val NEW_USER: User = User(
    "i3@gmail.com",
    "$2a$10$OLVVnNYAlUwFmjpSCoYnWeEkkeJBMQcNcKXR01m.gK.01McgpzudO",
    Some("New"),
    Some("User"),
    Some("IMG"),
    RECRUITER
  )

  val IMRAN_ADMIN_UPDATED: User = User(
    "i@gmail.com",
    "$2a$10$bzyhyAwIGeskJUXo9GL/L.cM2LSfnApMSCtxPK26ap4ZqXEVhmcO.",
    Some("Imran"),
    Some("Sarwar Butt"),
    Some("IMG"),
    ADMIN
  )

  val NEW_USER_IMRAN_ADMIN: NewUserInfo = NewUserInfo(
    imranEmail,
    imranPassword,
    Some("Imran"),
    Some("Sarwar"),
    Some("IMG")
  )

  val newUserEmail = "i3@gmail.com"
  val NEW_USER_CREATION: NewUserInfo = NewUserInfo(
    "i3@gmail.com",
    "pwd10",
    Some("New"),
    Some("User"),
    Some("IMG")
  )

  val mockedUser = new Users[IO]:
    override def find(email: String): IO[Option[User]] =
      if(email == imranEmail) IO.pure(Some(IMRAN_ADMIN))
      else IO.pure(None)
    override def create(user: User): IO[String] = IO.pure(user.email)
    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean] = IO.pure(true)