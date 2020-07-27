package pricemigrationengine.handlers

import pricemigrationengine.model.{CohortSpec, ConfigurationFailure, EmailSenderFailure, SalesforceClientFailure}
import pricemigrationengine.services._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{URLayer, ZLayer}

object LiveLayer {

  val logging: URLayer[Logging, Logging] = ZLayer.identity[Logging]

  private val dynamoDbClient: ZLayer[Logging, ConfigurationFailure, DynamoDBClient] =
    EnvConfiguration.dynamoDbImpl and logging to DynamoDBClient.dynamoDB

  def cohortTable(cohortSpec: CohortSpec): ZLayer[Logging, ConfigurationFailure, CohortTable] =
    dynamoDbClient and logging andTo
      DynamoDBZIOLive.impl and EnvConfiguration.cohortTableImp and EnvConfiguration.stageImp to
      CohortTableLive.impl(cohortSpec)

  val cohortTableDdl: ZLayer[Logging, ConfigurationFailure, CohortTableDdl] =
    Clock.live and dynamoDbClient and EnvConfiguration.stageImp and logging to CohortTableDdlLive.impl

  val cohortSpecTable: ZLayer[Logging, ConfigurationFailure, CohortSpecTable] =
    dynamoDbClient and EnvConfiguration.stageImp and logging to CohortSpecTableLive.impl

  val cohortStateMachine: ZLayer[Logging, ConfigurationFailure, CohortStateMachine] =
    Blocking.live and logging and EnvConfiguration.cohortStateMachineImpl to CohortStateMachineLive.impl

  val zuora: ZLayer[Logging, ConfigurationFailure, Zuora] =
    EnvConfiguration.zuoraImpl and logging to ZuoraLive.impl

  val salesforce: ZLayer[Logging, SalesforceClientFailure, SalesforceClient] =
    EnvConfiguration.salesforceImp and logging to SalesforceClientLive.impl

  val emailSender: ZLayer[Logging, EmailSenderFailure, EmailSender] =
    EnvConfiguration.emailSenderImp and logging to EmailSenderLive.impl

  val s3: ZLayer[Logging, ConfigurationFailure, S3] =
    logging to S3Live.impl

  val exportConfig: ZLayer[Any, ConfigurationFailure, ExportConfiguration] =
    EnvConfiguration.exportConfigImpl
}
