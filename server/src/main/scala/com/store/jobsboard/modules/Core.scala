package com.store.jobsboard.modules

import com.store.jobsboard.core.*
import cats.effect.*
import cats.implicits.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import com.store.jobsboard.config.SecurityConfig
import com.store.jobsboard.config.TokenConfig
import com.store.jobsboard.config.EmailServiceConfig

final class Core[F[_]] private (val jobs: Jobs[F], val users: Users[F], val auth: Auth[F])
object Core:
  // Made independent from one type of database
  def apply[F[_]: Async: Logger](xa: Transactor[F], tokenConfig: TokenConfig, emailServiceConfig: EmailServiceConfig): Resource[F, Core[F]] =
    val coreF = for {
      jobs  <- LiveJobs[F](xa)
      users <- LiveUser[F](xa)
      tokens <- LiveTokens[F](users)(xa, tokenConfig)
      emails <- LiveEmails[F](emailServiceConfig)
      auth  <- LiveAuth[F](users, tokens, emails)
    } yield new Core[F](jobs, users, auth)

    Resource.eval(coreF)
    // Resource.eval(LiveJobs[F](xa))
    //   .map(jobs => new Core(jobs))
