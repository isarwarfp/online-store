package com.store.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.store.jobsboard.fixtures.JobFixture
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import doobie.postgres.implicits.*
import doobie.implicits.*

class JobsSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with DoobieSpec with JobFixture:
  override val initScript: String = "sql/jobs.sql"

  "Jobs 'Algebra'" - {
    "should return empty if UUID does not exist" in {
      transactor.use { xa =>
        val program = for {
          jobs     <- LiveJobs[IO](xa)
          jobFound <- jobs.find(NotFoundJobUuid)
        } yield jobFound

        program.asserting(_ shouldBe None)
      }
    }
  }

  "should successfully find a job by UUID" in {
    transactor.use { xa =>
      val program = for {
        jobs     <- LiveJobs[IO](xa)
        jobFound <- jobs.find(AwesomeJobUuid)
      } yield jobFound

      program.asserting(_ shouldBe Some(AwesomeJob))
    }
  }

  "should successfully create a new job" in {
    transactor.use { xa =>
      val program = for {
        jobs     <- LiveJobs[IO](xa)
        jobId    <- jobs.create("abc@gmail.com", RockTheJvmNewJob)
        mayBeJob <- jobs.find(jobId)
      } yield mayBeJob

      program.asserting(_.map(_.jobInfo) shouldBe Some(RockTheJvmNewJob))
    }
  }

  "should successfully update an existing job" in {
    transactor.use { xa =>
      val program = for {
        jobs         <- LiveJobs[IO](xa)
        mayBeUpdated <- jobs.update(AwesomeJobUuid, UpdatedAwesomeJob.jobInfo)
      } yield mayBeUpdated

      program.asserting(_ shouldBe Some(UpdatedAwesomeJob))
    }
  }

  "should successfully delete a job" in {
    transactor.use { xa =>
      val program = for {
        jobs               <- LiveJobs[IO](xa)
        countOfDeletedJobs <- jobs.delete(AwesomeJobUuid)
        countOfJobs        <- sql"SELECT count(*) FROM jobs WHERE id = $AwesomeJobUuid"
          .query[Int].unique.transact(xa)
      } yield (countOfDeletedJobs, countOfJobs)

      program.asserting {
        case (countOfDeletedJobs, countOfJobs) =>
          countOfDeletedJobs shouldBe 1
          countOfJobs shouldBe 0
      }
    }
  }
