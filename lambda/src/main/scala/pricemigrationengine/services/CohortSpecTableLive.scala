package pricemigrationengine.services

import pricemigrationengine.model._
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import zio.{IO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

object CohortSpecTableLive {

  private val tableNamePrefix = "price-migration-engine-cohort-spec"

  val impl: ZLayer[DynamoDBClient with StageConfig with Logging, ConfigFailure, CohortSpecTable] =
    ZLayer.fromZIO(for {
      logging <- ZIO.service[Logging]
      stageConfig <- ZIO.service[StageConfig]
      dynamoDbClient <- ZIO.service[DynamoDBClient]
    } yield new CohortSpecTable {

      override val fetchAll: IO[Failure, Set[CohortSpec]] = {
        val scanRequest = ScanRequest.builder.tableName(s"$tableNamePrefix-${stageConfig.stage}").build()
        (for {
          scanResult <- dynamoDbClient
            .scan(scanRequest)
            .mapError(e => CohortSpecFetchFailure(s"Failed to fetch cohort specs: $e"))
          specs <- ZIO.foreach(scanResult.items.asScala.toList)(result =>
            ZIO
              .fromEither(CohortSpec.fromDynamoDbItem(result))
              .mapError(e => CohortSpecFetchFailure(s"Failed to parse '$result': ${e.reason}"))
          )
        } yield specs.toSet).tap(specs => logging.info(s"Fetched ${specs.size} cohort specs"))
      }

      override def update(spec: CohortSpec): ZIO[Any, CohortSpecUpdateFailure, Unit] =
        ZIO.fail(CohortSpecUpdateFailure("No implementation yet!"))
    })
}
