package com.store.jobsboard.playground

import cats.effect.*
import com.store.jobsboard.core.LiveJobs
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.*
import com.store.jobsboard.domain.job.JobInfo
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.io.StdIn

object JobsPlayground extends IOApp.Simple:

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:board",
      "docker",
      "docker",
      ec
    )
  } yield xa

  private val jobsInfo = JobInfo.minimal("Company", "SE", "Desc", "abc.com", true, "anywhere")

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs      <- LiveJobs[IO](xa)
      _         <- IO(println("Ready, Next...")) *> IO(StdIn.readLine)
      id        <- jobs.create("isarwar@gmail.com", jobsInfo)
      _         <- IO(println("Next...")) *> IO(StdIn.readLine)
      list      <- jobs.all()
      _         <- IO(println(s"All jobs: $jobs...")) *> IO(StdIn.readLine)
      _         <- jobs.update(id, jobsInfo.copy(title = "Senior SE"))
      newJob    <- jobs.find(id)
      _         <- IO(println(s"New job: $newJob")) *> IO(StdIn.readLine)
      _         <- jobs.delete(id)
      listAfter <- jobs.all()
      _         <- IO(println(s"All job: $listAfter")) *> IO(StdIn.readLine)
    } yield ()
  }
