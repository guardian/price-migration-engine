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

object StageConfiguration {
  trait Service {
    val config: IO[ConfigurationFailure, StageConfig]
  }

  val stageConfig: ZIO[StageConfiguration, ConfigurationFailure, StageConfig] =
    ZIO.accessM(_.get.config)
}

object EmailSenderConfiguration {
  trait Service {
    val config: IO[ConfigurationFailure, EmailSenderConfig]
  }

  val emailSenderConfig: ZIO[EmailSenderConfiguration, ConfigurationFailure, EmailSenderConfig] =
    ZIO.accessM(_.get.config)
}

object CohortStateMachineConfiguration {
  trait Service {
    val config: IO[ConfigurationFailure, CohortStateMachineConfig]
  }
  val cohortStateMachineConfig: ZIO[CohortStateMachineConfiguration, ConfigurationFailure, CohortStateMachineConfig] =
    ZIO.accessM(_.get.config)
}
