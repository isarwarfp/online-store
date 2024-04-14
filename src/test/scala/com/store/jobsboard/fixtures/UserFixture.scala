package com.store.jobsboard.fixtures

import com.store.jobsboard.domain.user.Role.*
import com.store.jobsboard.domain.user.User

trait UserFixture:
  val imranEmail = "i@gmail.com"
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