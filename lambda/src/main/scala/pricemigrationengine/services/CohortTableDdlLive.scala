package pricemigrationengine.services

import pricemigrationengine.model.{CohortSpec, CohortTableCreateFailure, ConfigurationFailure}
import software.amazon.awssdk.services.dynamodb.model.BillingMode.PAY_PER_REQUEST
import software.amazon.awssdk.services.dynamodb.model.KeyType.{HASH, RANGE}
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.S
import software.amazon.awssdk.services.dynamodb.model._
import zio.Schedule.{exponential, recurs}
import zio._

object CohortTableDdlLive {

  private val partitionKey = "subscriptionNumber"

  private val stageIndex = "ProcessingStageIndexV2"
  private val stageAndStartDateIndex = "ProcessingStageStartDateIndexV1"

  private val stageAttribute = "processingStage"
  private val startDateAttribute = "startDate"

  val impl: ZLayer[DynamoDBClient with StageConfiguration with Logging, ConfigurationFailure, CohortTableDdl] =
    ZLayer.fromZIO(
      for {
        logging <- ZIO.service[Logging]
        stageConfig <- StageConfiguration.stageConfig
        dynamoDbClient <- ZIO.service[DynamoDBClient]
      } yield new CohortTableDdl {

        private def create(tableName: String) = {
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

          dynamoDbClient.createTable(createRequest).mapError(e => CohortTableCreateFailure(e.toString))
        }

        private def enableContinuousBackups(tableName: String) = {
          val enableBackups = UpdateContinuousBackupsRequest.builder
            .tableName(tableName)
            .pointInTimeRecoverySpecification(
              PointInTimeRecoverySpecification.builder.pointInTimeRecoveryEnabled(true).build()
            )
            .build()

          val result = dynamoDbClient
            .updateContinuousBackups(enableBackups)
            .tapError(_ => logging.info(s"Waiting to enable continuous backups ..."))
            .retry(
              exponential(1.second) && recurs(8)
            )

          result.mapError(e => CohortTableCreateFailure(e.toString))
        }

        override def createTable(cohortSpec: CohortSpec): IO[CohortTableCreateFailure, Option[CreateTableResponse]] = {
          val tableName = cohortSpec.tableName(stageConfig.stage)
          for {
            // if table can be described, it must already exist and therefore not need to be created
            result <- dynamoDbClient
              .describeTable(tableName)
              .foldZIO(_ => create(tableName).map(Some(_)), _ => ZIO.none)
            _ <- enableContinuousBackups(tableName)
          } yield result
        }
      }
    )
}
