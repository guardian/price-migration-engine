package pricemigrationengine.handlers

import pricemigrationengine.model.{ConfigurationFailure, EmailSenderFailure, SalesforceClientFailure}
import pricemigrationengine.services._
import zio.blocking.Blocking
import zio.{URLayer, ZLayer}

object LiveLayer {

  val logging: URLayer[Logging, Logging] = ZLayer.identity[Logging]

  def cohortTable(tableName: String): ZLayer[Logging, ConfigurationFailure, CohortTable] =
    EnvConfiguration.dynamoDbImpl and logging andTo
      DynamoDBClient.dynamoDB andTo
      DynamoDBZIOLive.impl and EnvConfiguration.cohortTableImp and EnvConfiguration.stageImp to
      CohortTableLive.impl(tableName)

  val cohortSpecTable: ZLayer[Logging, ConfigurationFailure, CohortSpecTable] =
    logging and EnvConfiguration.dynamoDbImpl andTo
      DynamoDBClient.dynamoDB and EnvConfiguration.stageImp to
      CohortSpecTableLive.impl

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
}
