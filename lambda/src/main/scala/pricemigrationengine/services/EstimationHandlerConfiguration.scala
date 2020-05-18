package pricemigrationengine.services

import pricemigrationengine.model.{CohortTableConfig, EstimationHandlerConfig, ConfigurationFailure, DynamoDBConfig, ZuoraConfig}
import zio.{IO, ZIO}

object EstimationHandlerConfiguration {
  trait Service {
    val config: IO[ConfigurationFailure, EstimationHandlerConfig]
  }

  val estimationHandlerConfig: ZIO[EstimationHandlerConfiguration, ConfigurationFailure, EstimationHandlerConfig] =
    ZIO.accessM(_.get.config)
}

object ZuoraConfiguration {
  trait Service {
    val config: IO[ConfigurationFailure, ZuoraConfig]
  }

  val zuoraConfig: ZIO[ZuoraConfiguration, ConfigurationFailure, ZuoraConfig] =
    ZIO.accessM(_.get.config)
}

object DynamoDBConfiguration {
  trait Service {
    val config: IO[ConfigurationFailure, DynamoDBConfig]
  }

  val dynamoDBConfig: ZIO[DynamoDBConfiguration, ConfigurationFailure, DynamoDBConfig] =
    ZIO.accessM(_.get.config)
}

object CohortTableConfiguration {
  trait Service {
    val config: IO[ConfigurationFailure, CohortTableConfig]
  }

  val cohortTableConfig: ZIO[CohortTableConfiguration, ConfigurationFailure, CohortTableConfig] =
    ZIO.accessM(_.get.config)
}
