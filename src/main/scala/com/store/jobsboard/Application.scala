package com.store.jobsboard

import cats.*
import cats.effect.{IO, IOApp}
import cats.implicits.*
import com.store.foundations.Http4s.courseRoutes
import com.store.jobsboard.config.*
import com.store.jobsboard.config.syntax.*
import com.store.jobsboard.http.HttpApi
import com.store.jobsboard.http.routes.HealthRoutes
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource

import java.util.UUID

object Application extends IOApp.Simple:
  override def run: IO[Unit] =
  //IO.println(UUID.randomUUID())
    ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
      EmberServerBuilder
        .default[IO]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(HttpApi[IO].endpoints.orNotFound)
        .build // that will create resource
        .use(_ => IO.println("Server Started") *> IO.never)
    }

