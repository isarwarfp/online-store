package com.store.jobsboard.playground

import cats.effect.{IO, IOApp}
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

object PasswordHasingPlayground extends IOApp.Simple:
  override def run: IO[Unit] =
    BCrypt.hashpw[IO]("pwd").flatMap(IO.println) *>
      BCrypt.hashpw[IO]("pwd2").flatMap(IO.println) *>
      BCrypt.hashpw[IO]("pwd10").flatMap(IO.println)
//      BCrypt.checkpwBool[IO]("pwd", PasswordHash[BCrypt]("$2a$10$FqgaSbLZ5MEPQvD5qDTV5e0Xz/N8q3oT027ZwtLDsSIhMoQaMFGjC"))
//        .flatMap(IO.println)
