package pricemigrationengine.services

import java.lang.System.getenv
import java.time.LocalDate

import pricemigrationengine.model.{Config, ConfigurationFailure, DynamoDBConfig, DynamoDBEndpointConfig, SalesforceConfig, ZuoraConfig}
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
        stage <- env("stage")
        earliestStartDate <- env("earliestStartDate").map(LocalDate.parse)
        batchSize <- env("batchSize").map(_.toInt)
        zuoraApiHost <- env("zuoraApiHost")
        zuoraClientId <- env("zuoraClientId")
        zuoraClientSecret <- env("zuoraClientSecret")
        dynamoDBServiceEndpointOption <- optionalEnv("dynamodb.serviceEndpoint")
        dynamoDBSigningRegionOption <- optionalEnv("dynamodb.signingRegion")
        dynamoDBEndpoint = dynamoDBServiceEndpointOption
          .flatMap(endpoint => dynamoDBSigningRegionOption.map(region => DynamoDBEndpointConfig(endpoint, region)))
        salesforceClientId <- env("salesforceClientId")
        salesforceClientSecret <- env("salesforceClientSecret")
        salesforceUserName <- env("salesforceUserName")
        salesforcePassword <- env("salesforcePassword")
        salesforceToken <- env("salesforceToken")
      } yield
        Config(
          zuora = ZuoraConfig(
            zuoraApiHost,
            zuoraClientId,
            zuoraClientSecret
          ),
          dynamoDBConfig = DynamoDBConfig(
            dynamoDBEndpoint
          ),
          stage = stage,
          earliestStartDate = earliestStartDate,
          batchSize = batchSize,
          salesforce = SalesforceConfig(
            salesforceClientId,
            salesforceClientSecret,
            salesforceUserName,
            salesforcePassword,
            salesforceToken
          )
        )
    }
  }
}
