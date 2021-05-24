package pricemigrationengine.services

import pricemigrationengine.model.{CohortSpec, CohortTableCreateFailure, ConfigurationFailure}
import software.amazon.awssdk.services.dynamodb.model.BillingMode.PAY_PER_REQUEST
import software.amazon.awssdk.services.dynamodb.model.KeyType.{HASH, RANGE}
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.S
import software.amazon.awssdk.services.dynamodb.model._
import zio.Schedule.{exponential, recurs}
import zio.clock.Clock
import zio.duration._
import zio.{IO, ZIO, ZLayer}

object CohortTableDdlLive {

  private val partitionKey = "subscriptionNumber"

  private val stageIndex = "ProcessingStageIndexV2"
  private val stageAndStartDateIndex = "ProcessingStageStartDateIndexV1"

  private val stageAttribute = "processingStage"
  private val startDateAttribute = "startDate"

  val impl
      : ZLayer[DynamoDBClient with StageConfiguration with Clock with Logging, ConfigurationFailure, CohortTableDdl] =
    ZLayer.fromFunctionM { modules: DynamoDBClient with StageConfiguration with Clock with Logging =>
      StageConfiguration.stageConfig
        .map { stageConfig =>
          def create(tableName: String): ZIO[DynamoDBClient, CohortTableCreateFailure, CreateTableResponse] = {
            val createRequest = CreateTableRequest.builder
              .tableName(tableName)
              .keySchema(KeySchemaElement.builder.attributeName(partitionKey).keyType(HASH).build())
              .attributeDefinitions(
                AttributeDefinition.builder.attributeName(partitionKey).attributeType(S).build(),
                AttributeDefinition.builder.attributeName(stageAttribute).attributeType(S).build(),
                AttributeDefinition.builder
                  .attributeName(startDateAttribute)
                  .attributeType(S)
                  .build()
              )
              .globalSecondaryIndexes(
                GlobalSecondaryIndex.builder
                  .indexName(stageIndex)
                  .keySchema(KeySchemaElement.builder.attributeName(stageAttribute).keyType(HASH).build())
                  .projection(Projection.builder.projectionType(ProjectionType.ALL).build())
                  .build(),
                GlobalSecondaryIndex.builder
                  .indexName(stageAndStartDateIndex)
                  .keySchema(
                    KeySchemaElement.builder.attributeName(stageAttribute).keyType(HASH).build(),
                    KeySchemaElement.builder.attributeName(startDateAttribute).keyType(RANGE).build()
                  )
                  .projection(Projection.builder.projectionType(ProjectionType.ALL).build())
                  .build()
              )
              .billingMode(PAY_PER_REQUEST)
              .build()

            DynamoDBClient.createTable(createRequest).mapError(e => CohortTableCreateFailure(e.toString))
          }

          def enableContinuousBackups(tableName: String): ZIO[
            DynamoDBClient with Logging with Clock,
            CohortTableCreateFailure,
            UpdateContinuousBackupsResponse
          ] = {
            val enableBackups = UpdateContinuousBackupsRequest.builder
              .tableName(tableName)
              .pointInTimeRecoverySpecification(
                PointInTimeRecoverySpecification.builder.pointInTimeRecoveryEnabled(true).build()
              )
              .build()

            val result = DynamoDBClient
              .updateContinuousBackups(enableBackups)
              .tapError(_ => Logging.info(s"Waiting to enable continuous backups ..."))
              .retry(
                exponential(1.second) && recurs(8)
              ) // have to wait for table to be created before enabling backups

            result.mapError(e => CohortTableCreateFailure(e.toString))
          }

          new CohortTableDdl.Service {
            def createTable(cohortSpec: CohortSpec): IO[CohortTableCreateFailure, Option[CreateTableResponse]] = {
              val tableName = cohortSpec.tableName(stageConfig.stage)
              (for {
                // if table can be described, it must already exist and therefore not need to be created
                result <-
                  DynamoDBClient.describeTable(tableName).foldM(_ => create(tableName).map(Some(_)), _ => ZIO.none)
                _ <- enableContinuousBackups(tableName)
              } yield result).provide(modules)
            }
          }
        }
        .provide(modules)
    }
}
