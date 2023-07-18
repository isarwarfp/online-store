package com.store.foundations

import cats.effect.unsafe.implicits.global // platform where all fibers will run
import scala.concurrent.duration.*
import cats.effect.{IOApp, Spawn, Concurrent, Temporal}
import cats.effect.kernel.Resource
import scala.io.Source
import cats.MonadError
import cats.effect.kernel.MonadCancel
import cats.effect.kernel.Fiber
import cats.effect.kernel.GenSpawn
import cats.effect.kernel.Ref
import cats.effect.kernel.Deferred
import cats.Defer
import cats.effect.kernel.Sync
import scala.concurrent.ExecutionContext

object CatsEffect extends IOApp.Simple:
    // Describing Computation as Values

    // ####### IO #######
    // means: datastructure describing arbitrary computation (including side effects)
    import cats.effect.IO
    val firstIO: IO[Int] = IO.pure(313)
    val delayedIO: IO[Int] = IO.apply {
        println("IO takes thunk")
        313
    }
    
    def evalIO[A](io: IO[A]): Unit =
        val number = io.unsafeRunSync()
        println(s"Result here: $number")

    // Sleeping IO
    
    val sleepingIO = IO.sleep(1.second) *> IO(println("Woke UP 😃"))
    val evalSleepingIO = for {
        fib1 <- sleepingIO.start
        fib2 <- sleepingIO.start
        _ <- fib1.join *> fib2.join
    } yield ()

    // One fiber may cancel another fiber
    val cancelingFiber = for {
        fib1 <- sleepingIO.onCancel(IO(println("I am cancelled"))).start
        _    <- IO.sleep(500.millis) *> IO(println("I am cancelling other fiber")) *> fib1.cancel
    } yield ()

    // Reading Resource
    val resourceIO = Resource.make(IO(Source.fromFile("/Users/imran/Home/Github/online-store/src/main/scala/com/store/foundations/CatsEffect.scala")))
                    (source => IO(println("Closing Source")) *> IO(source.close()))
    val readingIO = resourceIO.use(source => IO(source.getLines().foreach(println)))

    // Abstract kind of computation
    // MonadCancel describes cancelable computation
    trait MyMonadCancel[F[_], E] extends MonadError[F, E]:
        trait CancellationFlagResetter:
            def apply[A](fa: F[A]): F[A] // With the cancellation flag reset
        
        def cancelled: F[Unit]
        def uncancelable[A](poll: CancellationFlagResetter => F[A]): F[A]

    val monadCancelIO: MonadCancel[IO, Throwable] = MonadCancel[IO]
    val uncancelableIO = monadCancelIO.uncancelable(_ => IO(313))

    // Spawn: Ability to create fibers
    trait MyGenSpawn[F[_], E] extends MonadError[F, E]:
        def start[A](fa: F[A]): F[Fiber[F, E, A]] // creates a fiber
        // We may also create, never, cede and racePair
    
    // Here simple type alias, that use E as Throwable
    trait MySpawn[F[_]] extends GenSpawn[F, Throwable]

    val spawnIO = Spawn[IO]
    val fiber = spawnIO.start(delayedIO) // creates that fiber

    // Concurrent means concurrencey primitives (atomic reference + promises)
    trait MyConcurrent[F[_]] extends Spawn[F]:
        def ref[A](a: A): F[Ref[F, A]]
        def deferred[A]: F[Deferred[F, A]]

    // Temporal: ability to suspend computation for a given time
    trait MyTemporal[F[_]] extends Concurrent[F]:
        def sleep(time: FiniteDuration): F[Unit]

    // Sync: ability to suspend synchrounous arbitrary expression in an effect
    trait MySync[F[_]] extends MonadCancel[F, Throwable] with Defer[F]:
        def delay[A](expression: => A): F[A]
        def blocking[A](expression: => A): F[A] // runs on a dedicated blocking thread pool

    // Async: ability to suspend asynchronous computation (i.e on other thread pools) into an effect managed by Cats Effect
    trait MyAsync[F[_]] extends Sync[F] with Temporal[F]:
        def executionContext: F[ExecutionContext]
        def async[A](cb: (Either[Throwable, A] => Unit) => F[Option[Unit]]): F[A]

    override def run: IO[Unit] =
        // (delayedIO)
        // evalSleepingIO
        // cancelingFiber
        readingIO

