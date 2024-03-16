package com.store.jobsboard.fixtures

import com.store.jobsboard.domain.user.Role.*
import com.store.jobsboard.domain.user.User

trait UserFixture:
  val IMRAN_ADMIN: User = User(
    "i@gmail.com",
    "pwd",
    Some("Imran"),
    Some("Sarwar"),
    Some("IMG"),
    ADMIN
  )

  val IMRAN_RECRUITER: User = User(
    "i2@gmail.com",
    "pwd2",
    Some("Imran"),
    Some("Sarwar"),
    Some("IMG"),
    RECRUITER
  )

  val NEW_USER: User = User(
    "i3@gmail.com",
    "pwd2",
    Some("New"),
    Some("User"),
    Some("IMG"),
    RECRUITER
  )

  val IMRAN_ADMIN_UPDATED: User = User(
    "i@gmail.com",
    "pwd10",
    Some("Imran"),
    Some("Sarwar Butt"),
    Some("IMG"),
    ADMIN
  )