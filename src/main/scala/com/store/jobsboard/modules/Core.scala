package com.store.jobsboard.modules

import com.store.jobsboard.core.*
import cats.effect.*
import cats.implicits.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import com.store.jobsboard.config.SecurityConfig

final class Core[F[_]] private (val jobs: Jobs[F], val auth: Auth[F])
object Core:
  // Made independent from one type of database
  def apply[F[_]: Async: Logger](xa: Transactor[F])(securityConfig: SecurityConfig): Resource[F, Core[F]] =
    val coreF = for {
      jobs  <- LiveJobs[F](xa)
      users <- LiveUser[F](xa)
      auth  <- LiveAuth[F](users)(securityConfig)
    } yield new Core[F](jobs, auth)

    Resource.eval(coreF)
    // Resource.eval(LiveJobs[F](xa))
    //   .map(jobs => new Core(jobs))
