package com.store.jobsboard.modules

import com.store.jobsboard.core.*
import cats.effect.*
import cats.implicits.*
import doobie.hikari.HikariTransactor
import doobie.util.*

final class Core[F[_]] private (val jobs: Jobs[F])
object Core:
  // Sequence of creation
  // postgress -> jobs -> core -> httpApi -> app
  def postgresResource[F[_]: Async]: Resource[F, HikariTransactor[F]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[F](
      "org.postgresql.Driver",
      "jdbc:postgresql:board",
      "docker",
      "docker",
      ec
    )
  } yield xa
  
  def apply[F[_]: Async]: Resource[F, Core[F]] =
    postgresResource[F]
      .evalMap(postgres => LiveJobs[F](postgres))
      .map(jobs => new Core(jobs))
