package pricemigrationengine.services

import java.lang.System.getenv

import pricemigrationengine.model.{Config, ConfigurationFailure, ZuoraConfig}
import zio.{IO, ZIO, ZLayer}

object EnvConfiguration {
  val impl: ZLayer[Any, Nothing, Configuration] = ZLayer.succeed {
    def env(name: String): IO[ConfigurationFailure, String] =
      ZIO
        .effect(getenv(name))
        .mapError(e => ConfigurationFailure(e.getMessage))
        .filterOrFail(Option(_).nonEmpty)(ConfigurationFailure(s"No value for '$name' in environment"))

    new Configuration.Service {
      val config: IO[ConfigurationFailure, Config] = for {
        apiHost <- env("zuora.apiHost")
        clientId <- env("zuora.clientId")
        clientSecret <- env("zuora.clientSecret")
      } yield
        Config(
          ZuoraConfig(
            apiHost,
            clientId,
            clientSecret
          )
        )
    }
  }
}
