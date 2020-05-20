package pricemigrationengine.services

import java.lang.System.getenv
import java.time.LocalDate

import pricemigrationengine.model.{CohortTableConfig, ConfigurationFailure, DynamoDBConfig, DynamoDBEndpointConfig, EstimationHandlerConfig, SalesforceConfig, ZuoraConfig}
import zio.{IO, ZIO, ZLayer}

object EnvConfiguration {
  def env(name: String): IO[ConfigurationFailure, String] =
    optionalEnv(name)
      .collect(ConfigurationFailure(s"No value for '$name' in environment")) {
        case Some(value) => value
      }

  def optionalEnv(name: String): IO[ConfigurationFailure, Option[String]] =
    ZIO
      .effect(Option(getenv(name)))
      .mapError(e => ConfigurationFailure(e.getMessage))

  val estimationImpl: ZLayer[Any, Nothing, EstimationHandlerConfiguration] = ZLayer.succeed {
    new EstimationHandlerConfiguration.Service {
      val config: IO[ConfigurationFailure, EstimationHandlerConfig] = for {
        stage <- env("stage")
        earliestStartDate <- env("earliestStartDate").map(LocalDate.parse)
        batchSize <- env("batchSize").map(_.toInt)
      } yield
        EstimationHandlerConfig(
          earliestStartDate,
        )
    }
  }

  val zuoraImpl: ZLayer[Any, Nothing, ZuoraConfiguration] = ZLayer.succeed {
    new ZuoraConfiguration.Service {
      val config: IO[ConfigurationFailure, ZuoraConfig] = for {
        zuoraApiHost <- env("zuoraApiHost")
        zuoraClientId <- env("zuoraClientId")
        zuoraClientSecret <- env("zuoraClientSecret")
      } yield
          ZuoraConfig(
            zuoraApiHost,
            zuoraClientId,
            zuoraClientSecret
          )
    }
  }

  val dynamoDbImpl: ZLayer[Any, Nothing, DynamoDBConfiguration] = ZLayer.succeed {
    new DynamoDBConfiguration.Service {
      val config: IO[ConfigurationFailure, DynamoDBConfig] = for {
        dynamoDBServiceEndpointOption <- optionalEnv("dynamodb.serviceEndpoint")
        dynamoDBSigningRegionOption <- optionalEnv("dynamodb.signingRegion")
        dynamoDBEndpoint = dynamoDBServiceEndpointOption
          .flatMap(endpoint => dynamoDBSigningRegionOption.map(region => DynamoDBEndpointConfig(endpoint, region)))
      } yield
        DynamoDBConfig(
          dynamoDBEndpoint
        )
    }
  }

  val cohortTableImp: ZLayer[Any, Nothing, CohortTableConfiguration] = ZLayer.succeed {
    new CohortTableConfiguration.Service {
      val config: IO[ConfigurationFailure, CohortTableConfig] = for {
        stage <- env("stage")
        batchSize <- env("batchSize").map(_.toInt)
      } yield
        CohortTableConfig(
          stage,
          batchSize
        )
    }
  }

  val salesforceImp: ZLayer[Any, Nothing, SalesforceConfiguration] = ZLayer.succeed {
    new SalesforceConfiguration.Service {
      val config: IO[ConfigurationFailure, SalesforceConfig] = for {
        salesforceAuthUrl <- env("salesforceAuthUrl")
        salesforceClientId <- env("salesforceClientId")
        salesforceClientSecret <- env("salesforceClientSecret")
        salesforceUserName <- env("salesforceUserName")
        salesforcePassword <- env("salesforcePassword")
        salesforceToken <- env("salesforceToken")
      } yield
        SalesforceConfig(
          salesforceAuthUrl,
          salesforceClientId,
          salesforceClientSecret,
          salesforceUserName,
          salesforcePassword,
          salesforceToken
        )
    }
  }

}
