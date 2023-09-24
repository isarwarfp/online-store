package com.store.jobsboard.http.routes

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.store.jobsboard.core.*
import com.store.jobsboard.domain.pagination.*
import com.store.jobsboard.domain.job.*
import com.store.jobsboard.fixtures.JobFixture
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.scalatest.freespec.{AnyFreeSpec, AsyncFreeSpec}
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

class JobRoutesSpec extends AsyncFreeSpec with AsyncIOSpec with Http4sDsl[IO] with Matchers with JobFixture:
  val jobs: Jobs[IO] = new Jobs[IO]:
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] = IO.pure(NewJobUuid)
    override def all(): IO[List[Job]] = IO.pure(List(AwesomeJob))
    override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] =
      if(filter.remote) IO.pure(List.empty)
      else IO.pure(List(AwesomeJob))
    override def find(id: UUID): IO[Option[Job]] =
      if(id == AwesomeJobUuid) IO.pure(Some(AwesomeJob)) else IO.pure(None)
    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if(id == AwesomeJobUuid) IO.pure(Some(UpdatedAwesomeJob)) else IO.pure(None)
    override def delete(id: UUID): IO[Int] =
      if(id == AwesomeJobUuid) IO.pure(1) else IO.pure(0)

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes

  "JobRoutes" - {
    "should return a job with a given id" in {
      for {
        response  <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        retrieved <- response.as[Job]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe AwesomeJob
      }
    }

    "should return all jobs" in {
      for {
        response <- jobRoutes.orNotFound.run(Request(method = Method.POST, uri = uri"jobs").withEntity(JobFilter()))
        retrieved <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(AwesomeJob)
      }
    }

    "should return all jobs that satisfy filter" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"jobs")
          .withEntity(JobFilter(remote = true))
        )
        retrieved <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List.empty
      }
    }

    "should successfully create job" in {
      for {
        response <- jobRoutes.orNotFound.run(Request(method = Method.POST, uri = uri"jobs/create")
          .withEntity(AwesomeJob.jobInfo))
        retrieved <- response.as[UUID]
      } yield {
        response.status shouldBe Status.Created
        retrieved shouldBe NewJobUuid
      }
    }

    "should update job if exists" in {
      for {
        response <- jobRoutes.orNotFound.run(Request(method = Method.PUT, uri = uri"jobs/843df718-ec6e-4d49-9289-f799c0f40064")
          .withEntity(UpdatedAwesomeJob.jobInfo))
        invalidResponse <- jobRoutes.orNotFound.run(Request(method = Method.PUT, uri = uri"jobs/843df718-ec6e-4d49-9289-000000000000")
          .withEntity(UpdatedAwesomeJob.jobInfo))
      } yield {
        response.status shouldBe Status.Ok
        invalidResponse.status shouldBe Status.NotFound
      }
    }

    "should delete job if exists" in {
      for {
        response <- jobRoutes.orNotFound.run(Request(method = Method.DELETE, uri = uri"jobs/843df718-ec6e-4d49-9289-f799c0f40064")
          .withEntity(UpdatedAwesomeJob.jobInfo))
        invalidResponse <- jobRoutes.orNotFound.run(Request(method = Method.DELETE, uri = uri"jobs/843df718-ec6e-4d49-9289-000000000000")
          .withEntity(UpdatedAwesomeJob.jobInfo))
      } yield {
        response.status shouldBe Status.Ok
        invalidResponse.status shouldBe Status.NotFound
      }
    }
  }