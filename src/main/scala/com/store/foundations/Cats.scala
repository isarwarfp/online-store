package com.store.foundations

/**
  * Type classes:
     - Applicative
     - Functor
     - FlatMap
     - Monad
     - ApplicativeError / MonadError
  */
object Cats:
    // ####### Functor #######
    // means: mappable structures
    trait MyFunctor[F[_]]:
      def map[A, B](initVal: F[A])(f: A => B): F[B]

    import cats.Functor
    import cats.instances.list.*
    val listFunctor = Functor[List]

    // generalizeable, mappable APIs
    def increment[F[_]](container: F[Int])(using functor: Functor[F]): F[Int] =
      functor.map(container)(_ + 1)


    import cats.syntax.functor.*
    def increment_v2[F[_]](container: F[Int])(using functor: Functor[F]): F[Int] =
      container.map(_ + 1)

    // ####### Applicative #######
    // means: pure, wrap existing values into wrapper values
    trait MyApplicative[F[_]] extends Functor[F]:
      def pure[A](value: A): F[A]

    import cats.Applicative
    val applicativeList = Applicative[List]
    val aSimpleList = applicativeList.pure(313)

    import cats.syntax.applicative.*
    val aSimpleList_v2 = 313.pure[List]

    // ####### Applicative #######
    // means: that flatten values and map them
    trait MyFlatmap[F[_]] extends MyFunctor[F]:
      def flatMap[A, B](initVal: F[A])(using f: A => F[B]): F[B]

    import cats.FlatMap
    val flatMapList = FlatMap[List]
    val flatMappedList = flatMapList.flatMap(List(1, 2, 3))(x => List(x, x * 2))

    import cats.syntax.flatMap.*
    def crossProduct[F[_]: FlatMap, A, B](containerA: F[A], containerB: F[B]): F[(A, B)] = 
      containerA.flatMap(a => containerB.map(b => (a, b)))

    // ####### Monad #######
    // means: datatype generated from 'FlatMap + Applicative'
    trait MyMonad[F[_]] extends Applicative[F] with FlatMap[F]:
      // Using flatmap and pure, we can create 'Map' function
      override def map[A, B](fa: F[A])(f: A => B): F[B] =
        flatMap(fa)(a => pure(f(a)))

    import cats.Monad
    val monadList = cats.Monad[List]
    
    import cats.syntax.monad.*
    def crossProduct_v2[F[_]: Monad, A, B](containerA: F[A], containerB: F[B]): F[(A, B)] = 
      for {
        a <- containerA
        b <- containerB
      } yield (a, b)

    // ####### Applicative Error #######
    // means: computation that can fail
    trait MyApplicativeError[F[_], E] extends Applicative[F]:
      def raiseError[A](error: E): F[A]

    // examples as of AppicativeError are like, Try and Either
    import cats.ApplicativeError
    type ErrorOr[A] = Either[String, A]
    val applicativeEither = ApplicativeError[ErrorOr, String]
    val desiredValue = applicativeEither.pure(313)
    val failedValue = applicativeEither.raiseError("Couldn't parse value")

    import cats.syntax.applicativeError.*
    val desiredValue_v2 = 313.pure
    val failedValue_v2 = "Couldn't found value".raiseError

    // ####### Monad Error #######
    // means: it has a flatmap too and all powers as Monad
    trait MyMonadError[F[_], E] extends MyApplicativeError[F, E] with Monad[F]
    
    import cats.MonadError
    val monadError = MonadError[ErrorOr, String]


    @main def main = 
      println(increment(List(1, 2, 3)))
      println(aSimpleList)
      println(flatMappedList)
      println(crossProduct(List(1, 2), List(3, 4)))
      println(crossProduct_v2(List(1, 2), List(5, 6)))

      println(desiredValue)
      println(failedValue)