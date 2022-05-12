package pricemigrationengine.services

import pricemigrationengine.model._
import zio.{Layer, ZIO, ZLayer}

import java.lang.System.getenv

/** Configuration settings found in system environment.
  */
object EnvConfig {

  private def env(name: String) =
    for {
      opt <- optionalEnv(name)
      value <- ZIO.fromOption(opt).orElseFail(ConfigFailure(s"No value for '$name' in environment"))
    } yield value

  private def optionalEnv(name: String) =
    ZIO.attempt(Option(getenv(name))).mapError(e => ConfigFailure(e.getMessage))

  object zuora {
    val layer: Layer[ConfigFailure, ZuoraConfig] = ZLayer.fromZIO(
      for {
        zuoraApiHost <- env("zuoraApiHost")
        zuoraClientId <- env("zuoraClientId")
        zuoraClientSecret <- env("zuoraClientSecret")
      } yield ZuoraConfig(
        zuoraApiHost,
        zuoraClientId,
        zuoraClientSecret
      )
    )
  }

  object cohortTable {
    val layer: Layer[ConfigFailure, CohortTableConfig] = ZLayer.fromZIO(
      for {
        batchSize <- env("batchSize").map(_.toInt)
      } yield CohortTableConfig(batchSize)
    )
  }

  object salesforce {
    val layer: Layer[ConfigFailure, SalesforceConfig] = ZLayer.fromZIO(
      for {
        salesforceAuthUrl <- env("salesforceAuthUrl")
        salesforceClientId <- env("salesforceClientId")
        salesforceClientSecret <- env("salesforceClientSecret")
        salesforceUserName <- env("salesforceUserName")
        salesforcePassword <- env("salesforcePassword")
        salesforceToken <- env("salesforceToken")
      } yield SalesforceConfig(
        salesforceAuthUrl,
        salesforceClientId,
        salesforceClientSecret,
        salesforceUserName,
        salesforcePassword,
        salesforceToken
      )
    )
  }

  object stage {
    val layer: Layer[ConfigFailure, StageConfig] = ZLayer.fromZIO(
      for {
        stage <- env("stage")
      } yield StageConfig(stage)
    )
  }

  object emailSender {
    val layer: Layer[ConfigFailure, EmailSenderConfig] = ZLayer.fromZIO(
      for {
        emailSqsQueueName <- env("sqsEmailQueueName")
      } yield EmailSenderConfig(emailSqsQueueName)
    )
  }

  object cohortStateMachine {
    val layer: Layer[ConfigFailure, CohortStateMachineConfig] = ZLayer.fromZIO(
      for {
        stateMachineArn <- env("cohortStateMachineArn")
      } yield CohortStateMachineConfig(stateMachineArn)
    )
  }

  object `export` {
    val layer: Layer[ConfigFailure, ExportConfig] = ZLayer.fromZIO(
      for {
        exportBucketName <- env("exportBucketName")
      } yield ExportConfig(exportBucketName)
    )
  }
}
