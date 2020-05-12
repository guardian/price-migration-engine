package pricemigrationengine.services

import pricemigrationengine.model.{Config, ConfigurationFailure}
import zio.{IO, ZIO}

object Configuration {

  trait Service {
    val config: IO[ConfigurationFailure, Config]
  }

  val config: ZIO[Configuration, ConfigurationFailure, Config] =
    ZIO.accessM(_.get.config)
}
