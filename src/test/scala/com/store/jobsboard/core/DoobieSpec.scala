package com.store.jobsboard.core

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import doobie.implicits.*
import org.testcontainers.containers.PostgreSQLContainer


trait DoobieSpec:
  // Simulate database, using test containers
  val initScript: String
  val postgres: Resource[IO, PostgreSQLContainer[Nothing]] = {
    val acquire = IO {
      val container: PostgreSQLContainer[Nothing] = new PostgreSQLContainer("postgres")
        .withInitScript(initScript)
      container.start()
      container
    }
    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())
    Resource.make(acquire)(release)
  }
  // Step 1: Setup Postgres Transactor
  val transactor: Resource[IO, Transactor[IO]] = for {
    db <- postgres
    ec <- ExecutionContexts.fixedThreadPool[IO](1)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      db.getJdbcUrl,
      db.getUsername,
      db.getPassword,
      ec
    )
  } yield xa
