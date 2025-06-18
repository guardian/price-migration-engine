package pricemigrationengine.handlers

import org.apache.commons.csv.{CSVFormat, CSVParser}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.stream.ZStream
import zio.{Clock, IO, ZIO}

import java.io.{InputStream, InputStreamReader}
import scala.jdk.CollectionConverters._

object SubscriptionIdUploadHandler extends CohortHandler {

  private val csvFormat = CSVFormat.Builder.create().get()

  private def sourceLocation(cohortSpec: CohortSpec): ZIO[StageConfig, ConfigFailure, S3Location] =
    ZIO.service[StageConfig] map { stageConfig =>
      S3Location(
        s"price-migration-engine-${stageConfig.stage.toLowerCase}",
        s"${cohortSpec.normalisedCohortName}"
      )
    }

  private def sourceSublocation(
      cohortSpec: CohortSpec,
      sublocation: String
  ): ZIO[StageConfig, ConfigFailure, S3Location] =
    sourceLocation(cohortSpec).map(loc => loc.copy(key = s"${loc.key}/$sublocation"))

  private def importSourceLocation(cohortSpec: CohortSpec) =
    sourceSublocation(cohortSpec, "subscription-numbers.csv")

  def main(
      cohortSpec: CohortSpec
  ): ZIO[CohortTable with S3 with StageConfig with Logging, Failure, HandlerOutput] =
    (for {
      today <- Clock.currentDateTime.map(_.toLocalDate)
      _ <-
        if (today.isBefore(cohortSpec.importStartDate))
          Logging.info(s"No action. Import start date ${cohortSpec.importStartDate} is in the future.").unit
        else
          importCohortAndCleanUp(cohortSpec)
    } yield HandlerOutput(isComplete = true))
      .tapError(e => Logging.error(e.toString))

  def importCohortAndCleanUp(
      cohortSpec: CohortSpec
  ): ZIO[CohortTable with S3 with StageConfig with Logging, Failure, Unit] =
    for {
      _ <- importCohort(cohortSpec).catchSome { case e: S3Failure =>
        Logging.info(s"No action.  Cohort already imported: ${e.reason}").unit
      }
      src <- sourceLocation(cohortSpec)
      _ <- S3.deleteObject(src)
    } yield ()

  private def importCohort(
      cohortSpec: CohortSpec
  ): ZIO[CohortTable with Logging with S3 with StageConfig, Failure, Unit] =
    ZIO.scoped(for {
      importSrc <- importSourceLocation(cohortSpec)
      subscriptionIdsManagedStream <- S3.getObject(importSrc)
      inclusionStream <- subscriptionIdsManagedStream
      count <- writeSubscriptionIdsToCohortTable(inclusionStream)
      _ <- Logging.info(s"Wrote $count subscription ids to the cohort table")
    } yield ())

  def writeSubscriptionIdsToCohortTable(inputStream: InputStream): ZIO[CohortTable with Logging, Failure, Long] = {
    ZStream
      .fromJavaIterator(
        CSVParser.parse(new InputStreamReader(inputStream, "UTF-8"), csvFormat).iterator()
      )
      .mapBoth(
        ex => SubscriptionIdUploadFailure(s"Failed to read subscription csv stream: $ex"),
        csvRecord => csvRecord.get(0)
      )
      .mapZIO { subscriptionId =>
        ({
          CohortTable
            .create(CohortItem(subscriptionId, ReadyForEstimation))
        } <* Logging.info(s"Imported subscription $subscriptionId"))
          .catchSome { case e: CohortItemAlreadyPresentFailure =>
            Logging.info(s"Ignored $subscriptionId as already in table (DB error: ${e.reason})").unit
          }
          .tapError(e => Logging.error(s"Subscription $subscriptionId failed: $e"))
      }
      .runCount
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    MigrationType(input) match {
      case Newspaper2025P1 => ZIO.succeed(HandlerOutput(isComplete = true))
      case _ => {
        main(input).provideSome[Logging](
          EnvConfig.cohortTable.layer,
          EnvConfig.stage.layer,
          DynamoDBClientLive.impl,
          DynamoDBZIOLive.impl,
          CohortTableLive.impl(input),
          S3Live.impl
        )
      }
    }
  }
}
