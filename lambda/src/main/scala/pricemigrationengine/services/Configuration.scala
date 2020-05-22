package pricemigrationengine.services

import pricemigrationengine.model._
import zio.{IO, ZIO}

object AmendmentConfiguration {
  trait Service {
    val config: IO[ConfigurationFailure, AmendmentConfig]
  }

  val amendmentConfig: ZIO[AmendmentConfiguration, ConfigurationFailure, AmendmentConfig] =
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

object SalesforceConfiguration {
  trait Service {
    val config: IO[ConfigurationFailure, SalesforceConfig]
  }

  val salesforceConfig: ZIO[SalesforceConfiguration, ConfigurationFailure, SalesforceConfig] =
    ZIO.accessM(_.get.config)
}
