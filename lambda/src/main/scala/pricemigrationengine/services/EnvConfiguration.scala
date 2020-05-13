package pricemigrationengine.services

import java.lang.System.getenv

import pricemigrationengine.model.{Config, ConfigurationFailure, DynamoDBConfig, DynamoDBEndpointConfig, ZuoraConfig}
import zio.{IO, ZIO, ZLayer}

object EnvConfiguration {
  val impl: ZLayer[Any, Nothing, Configuration] = ZLayer.succeed {
    def env(name: String): IO[ConfigurationFailure, String] =
      optionalEnv(name)
        .collect(ConfigurationFailure(s"No value for '$name' in environment")) {
          case Some(value) => value
        }

    def optionalEnv(name: String): IO[ConfigurationFailure, Option[String]] =
      ZIO
        .effect(Option(getenv(name)))
        .mapError(e => ConfigurationFailure(e.getMessage))

    new Configuration.Service {
      val config: IO[ConfigurationFailure, Config] = for {
        stage <- env("Stage")
        apiHost <- env("zuoraApiHost")
        clientId <- env("zuoraClientId")
        clientSecret <- env("zuoraClientSecret")
        dynamoDBServiceEndpointOption <- optionalEnv("dynamodbServiceEndpoint")
        dynamoDBSigningRegionOption <- optionalEnv("dynamodbSigningRegion")
        dynamoDBEndpoint = dynamoDBServiceEndpointOption
          .flatMap(endpoint => dynamoDBSigningRegionOption.map(region => DynamoDBEndpointConfig(endpoint, region)))
      } yield
        Config(
          ZuoraConfig(
            apiHost,
            clientId,
            clientSecret
          ),
          DynamoDBConfig(
            dynamoDBEndpoint
          ),
          stage
        )
    }
  }
}
