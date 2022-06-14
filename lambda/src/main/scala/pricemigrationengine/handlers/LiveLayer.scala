package pricemigrationengine.handlers

import pricemigrationengine.model.{CohortSpec, ConfigFailure, Failure}
import pricemigrationengine.services._
import zio.{URLayer, ZLayer}

object LiveLayer {

  val logging: URLayer[Logging, Logging] = ZLayer.environment[Logging]

  private val dynamoDbClient: ZLayer[Logging, ConfigFailure, DynamoDBClient] =
    logging to DynamoDBClientLive.impl

  def cohortTable(cohortSpec: CohortSpec): ZLayer[Logging, ConfigFailure, CohortTable] =
    dynamoDbClient and logging andTo
      DynamoDBZIOLive.impl and EnvConfig.cohortTable.layer and EnvConfig.stage.layer to
      CohortTableLive.impl(cohortSpec)

  val cohortTableDdl: ZLayer[Logging, ConfigFailure, CohortTableDdl] =
    dynamoDbClient and EnvConfig.stage.layer and logging to CohortTableDdlLive.impl

  val cohortSpecTable: ZLayer[Logging, ConfigFailure, CohortSpecTable] =
    dynamoDbClient and EnvConfig.stage.layer and logging to CohortSpecTableLive.impl

  val cohortStateMachine: ZLayer[Logging, ConfigFailure, CohortStateMachine] =
    logging and EnvConfig.cohortStateMachine.layer to CohortStateMachineLive.impl

  val zuora: ZLayer[Logging, ConfigFailure, Zuora] =
    EnvConfig.zuora.layer and logging to ZuoraLive.impl

  val salesforce: ZLayer[Logging, Failure, SalesforceClient] =
    EnvConfig.salesforce.layer and logging to SalesforceClientLive.impl
}
