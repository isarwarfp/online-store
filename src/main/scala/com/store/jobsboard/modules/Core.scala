package com.store.jobsboard.modules

import com.store.jobsboard.core.*
import cats.effect.*
import cats.implicits.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import doobie.util.transactor.Transactor

final class Core[F[_]] private (val jobs: Jobs[F])
object Core:
  // Made independent from one type of database
  def apply[F[_]: Async](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource.eval(LiveJobs[F](xa))
      .map(jobs => new Core(jobs))
