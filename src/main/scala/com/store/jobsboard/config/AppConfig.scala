package com.store.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class AppConfig(
    postgresConfig: PostgresConfig,
    emberConfig: EmberConfig,
    securityConfig: SecurityConfig
) derives ConfigReader
