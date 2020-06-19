package pricemigrationengine.services

import java.time.LocalDate

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, ScanRequest}
import pricemigrationengine.model._
import zio.{IO, ZIO, ZLayer}

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object CohortSpecTableLive {

  private val tableNamePrefix = "price-migration-engine-cohort-spec"

  private def toCohortSpec(values: mutable.Map[String, AttributeValue]): Either[CohortSpecFetchFailure, CohortSpec] = {

    def stringFieldValue(fields: mutable.Map[String, AttributeValue],
                         fieldName: String): Either[CohortSpecFetchFailure, String] =
      fields.get(fieldName).map(_.getS).toRight(CohortSpecFetchFailure(s"Can't read field '$fieldName'"))

    def optStringFieldValue(fields: mutable.Map[String, AttributeValue], fieldName: String): Option[String] =
      fields.get(fieldName).map(_.getS)

    for {
      cohortName <- stringFieldValue(values, "cohortName")
      earliestPriceMigrationStartDate <- stringFieldValue(values, "earliestPriceMigrationStartDate").map(
        LocalDate.parse)
      importStartDate <- stringFieldValue(values, "importStartDate").map(LocalDate.parse)
      migrationCompleteDate <- Right(optStringFieldValue(values, "migrationCompleteDate").map(LocalDate.parse))
    } yield
      CohortSpec(
        cohortName,
        earliestPriceMigrationStartDate,
        importStartDate,
        migrationCompleteDate
      )
  }

  val impl: ZLayer[DynamoDBClient with StageConfiguration with Logging, ConfigurationFailure, CohortSpecTable] =
    ZLayer.fromFunction { modules: DynamoDBClient with StageConfiguration with Logging =>
      new CohortSpecTable.Service {

        val fetchAll: IO[Failure, Set[CohortSpec]] = {
          (for {
            stageConfig <- StageConfiguration.stageConfig
            scanRequest = new ScanRequest().withTableName(s"$tableNamePrefix-${stageConfig.stage}")
            scanResult <- DynamoDBClient
              .scan(scanRequest)
              .mapError(e => CohortSpecFetchFailure(s"Failed to fetch cohort specs: $e"))
            specs <- ZIO.foreach(scanResult.getItems.asScala)(
              result =>
                ZIO
                  .fromEither(toCohortSpec(result.asScala))
                  .mapError(e => CohortSpecFetchFailure(s"Failed to parse '$result': ${e.reason}")))
          } yield specs.toSet).tap(specs => Logging.info(s"Fetched ${specs.size} cohort specs"))
        }.provide(modules)

        def update(spec: CohortSpec): ZIO[Any, CohortSpecUpdateFailure, Unit] =
          ZIO.fail(CohortSpecUpdateFailure("No implementation yet!"))
      }
    }
}
