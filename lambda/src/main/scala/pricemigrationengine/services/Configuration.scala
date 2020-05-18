package pricemigrationengine.services

import pricemigrationengine.model.{Config, ConfigurationFailure, ZuoraConfig}
import zio.{IO, ZIO}

object Configuration {

  trait Service {
    val config: IO[ConfigurationFailure, Config]
  }

  val config: ZIO[Configuration, ConfigurationFailure, Config] =
    ZIO.accessM(_.get.config)
}

object ZuoraConfiguration {

  trait Service {
    val config: IO[ConfigurationFailure, ZuoraConfig]
  }

  val zuoraConfig: ZIO[ZuoraConfiguration, ConfigurationFailure, ZuoraConfig] =
    ZIO.accessM(_.get.config)
}
