package pricemigrationengine.services

import com.amazonaws.services.dynamodbv2.model.BillingMode.PAY_PER_REQUEST
import com.amazonaws.services.dynamodbv2.model.KeyType.{HASH, RANGE}
import com.amazonaws.services.dynamodbv2.model.ProjectionType.ALL
import com.amazonaws.services.dynamodbv2.model._
import pricemigrationengine.model.{CohortSpec, CohortTableCreateFailure, ConfigurationFailure}
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

  private val stringType = "S"

  val impl
      : ZLayer[DynamoDBClient with StageConfiguration with Clock with Logging, ConfigurationFailure, CohortTableDdl] =
    ZLayer.fromFunctionM { modules: DynamoDBClient with StageConfiguration with Clock with Logging =>
      StageConfiguration.stageConfig
        .map { stageConfig =>
          def create(tableName: String): ZIO[DynamoDBClient, CohortTableCreateFailure, CreateTableResult] = {
            val createRequest = new CreateTableRequest()
              .withTableName(tableName)
              .withKeySchema(new KeySchemaElement().withAttributeName(partitionKey).withKeyType(HASH))
              .withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(partitionKey).withAttributeType(stringType),
                new AttributeDefinition().withAttributeName(stageAttribute).withAttributeType(stringType),
                new AttributeDefinition().withAttributeName(startDateAttribute).withAttributeType(stringType)
              )
              .withGlobalSecondaryIndexes(
                new GlobalSecondaryIndex()
                  .withIndexName(stageIndex)
                  .withKeySchema(new KeySchemaElement().withAttributeName(stageAttribute).withKeyType(HASH))
                  .withProjection(new Projection().withProjectionType(ALL)),
                new GlobalSecondaryIndex()
                  .withIndexName(stageAndStartDateIndex)
                  .withKeySchema(
                    new KeySchemaElement().withAttributeName(stageAttribute).withKeyType(HASH),
                    new KeySchemaElement().withAttributeName(startDateAttribute).withKeyType(RANGE)
                  )
                  .withProjection(new Projection().withProjectionType(ALL))
              )
              .withBillingMode(PAY_PER_REQUEST)

            DynamoDBClient.createTable(createRequest).mapError(e => CohortTableCreateFailure(e.toString))
          }

          def enableContinuousBackups(tableName: String): ZIO[
            DynamoDBClient with Logging with Clock,
            CohortTableCreateFailure,
            UpdateContinuousBackupsResult
          ] = {
            val enableBackups =
              new UpdateContinuousBackupsRequest()
                .withTableName(tableName)
                .withPointInTimeRecoverySpecification(
                  new PointInTimeRecoverySpecification().withPointInTimeRecoveryEnabled(true)
                )

            val result = DynamoDBClient
              .updateContinuousBackups(enableBackups)
              .tapError(_ => Logging.info(s"Waiting to enable continuous backups ..."))
              .retry(
                exponential(1.second) && recurs(8)
              ) // have to wait for table to be created before enabling backups

            result.mapError(e => CohortTableCreateFailure(e.toString))
          }

          //noinspection ConvertExpressionToSAM
          new CohortTableDdl.Service {
            def createTable(cohortSpec: CohortSpec): IO[CohortTableCreateFailure, Option[CreateTableResult]] = {
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
