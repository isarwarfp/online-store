package com.store.foundations

import cats.{Applicative, Functor}
import cats.instances.list.given
import cats.instances.option.given
import cats.syntax.either.given

object Practice extends App:
  def increment[F[_]](container: F[Int])(using func: Functor[F]): F[Int] =
    func.map(container)(_ + 1)

  println(increment(List(1, 2)))


  import cats.syntax.applicative.*
  val applicative = Applicative[List]

  val x = 2.pure[List]
  println(x)
