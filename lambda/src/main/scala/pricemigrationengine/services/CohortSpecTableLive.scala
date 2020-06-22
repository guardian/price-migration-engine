package pricemigrationengine.services

import java.util

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, ScanRequest}
import pricemigrationengine.model._
import pricemigrationengine.model.dynamodb.Conversions._
import zio.{IO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

object CohortSpecTableLive {

  private val tableNamePrefix = "price-migration-engine-cohort-spec"

  private def toCohortSpec(values: util.Map[String, AttributeValue]): Either[CohortSpecFetchFailure, CohortSpec] = {
    def errorMapped[A](orig: Either[String, A]): Either[CohortSpecFetchFailure, A] =
      orig.left.map(e => CohortSpecFetchFailure(e))
    for {
      cohortName <- errorMapped(getStringFromResults(values, "cohortName"))
      earliestPriceMigrationStartDate <- errorMapped(getDateFromResults(values, "earliestPriceMigrationStartDate"))
      importStartDate <- errorMapped(getDateFromResults(values, "importStartDate"))
      migrationCompleteDate <- errorMapped(getOptionalDateFromResults(values, "migrationCompleteDate"))
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
                  .fromEither(toCohortSpec(result))
                  .mapError(e => CohortSpecFetchFailure(s"Failed to parse '$result': ${e.reason}")))
          } yield specs.toSet).tap(specs => Logging.info(s"Fetched ${specs.size} cohort specs"))
        }.provide(modules)

        def update(spec: CohortSpec): ZIO[Any, CohortSpecUpdateFailure, Unit] =
          ZIO.fail(CohortSpecUpdateFailure("No implementation yet!"))
      }
    }
}
