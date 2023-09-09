package com.store.jobsboard

import cats.*
import cats.effect.{IO, IOApp}
import cats.implicits.*
import com.store.foundations.Http4s.courseRoutes
import com.store.jobsboard.config.*
import com.store.jobsboard.config.syntax.*
import com.store.jobsboard.http.routes.HealthRoutes
import com.store.jobsboard.modules.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource

import java.util.UUID

object Application extends IOApp.Simple:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = ConfigSource.default.loadF[IO, AppConfig].flatMap {
    case AppConfig(postgresConfig, emberConfig) =>
      val appResource = for {
        xa     <- Database.mkPostgres[IO](postgresConfig)
        core   <- Core[IO](xa)
        api    <- HttpApi[IO](core)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(emberConfig.host)
          .withPort(emberConfig.port)
          .withHttpApp(api.endpoints.orNotFound)
          .build // that will create resource
      } yield server
      appResource.use(_ => IO.println("Server Started") *> IO.never)
  }

