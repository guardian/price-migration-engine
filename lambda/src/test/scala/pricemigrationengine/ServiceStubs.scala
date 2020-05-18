package pricemigrationengine

import java.time.LocalDate

import pricemigrationengine.model.{Config, ConfigurationFailure, DynamoDBConfig, SalesforceConfig, ZuoraConfig}
import pricemigrationengine.services.{Configuration, ConsoleLogging}
import zio.{IO, ZLayer, console}

object ServiceStubs {
  val stubConfig: Config = Config(
    ZuoraConfig("", "", ""),
    DynamoDBConfig(None),
    "DEV",
    LocalDate.now,
    batchSize = 101,
    salesforce = SalesforceConfig(
      "",
      "",
      "",
      "",
      ""
    )
  )

  val stubConfigurationLayer = ZLayer.succeed(
    new Configuration.Service {
      override val config: IO[ConfigurationFailure, Config] =
        IO.succeed(
          stubConfig
        )
    }
  )

  val stubLoggingLayer = console.Console.live >>> ConsoleLogging.impl
}
