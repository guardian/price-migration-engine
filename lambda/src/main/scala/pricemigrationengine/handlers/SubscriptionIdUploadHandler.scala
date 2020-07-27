package pricemigrationengine.handlers

import java.io.{InputStream, InputStreamReader}

import org.apache.commons.csv.{CSVFormat, CSVParser}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.stream.ZStream
import zio.{IO, ZEnv, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

object SubscriptionIdUploadHandler extends CohortHandler {

  private val csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader()

  private def sourceLocation(cohortSpec: CohortSpec): ZIO[StageConfiguration, ConfigurationFailure, S3Location] =
    StageConfiguration.stageConfig map { stageConfig =>
      S3Location(
        s"price-migration-engine-${stageConfig.stage.toLowerCase}",
        s"${cohortSpec.normalisedCohortName}"
      )
    }

  private def sourceSublocation(
      cohortSpec: CohortSpec,
      sublocation: String
  ): ZIO[StageConfiguration, ConfigurationFailure, S3Location] =
    sourceLocation(cohortSpec).map(loc => loc.copy(key = s"${loc.key}/$sublocation"))

  private def importSourceLocation(cohortSpec: CohortSpec) =
    sourceSublocation(cohortSpec, "salesforce-subscription-id-report.csv")

  private def exclusionSourceLocation(cohortSpec: CohortSpec) =
    sourceSublocation(cohortSpec, "excluded-subscription-ids.csv")

  def main(
      cohortSpec: CohortSpec
  ): ZIO[CohortTable with S3 with StageConfiguration with Logging, Failure, HandlerOutput] =
    for {
      _ <- importCohort(cohortSpec).catchSome {
        case e: S3Failure => Logging.info(s"No action.  Cohort already imported: ${e.reason}") zipRight ZIO.succeed(())
      }
      src <- sourceLocation(cohortSpec)
      _ <- S3.deleteObject(src)
    } yield HandlerOutput(isComplete = true)

  private def importCohort(
      cohortSpec: CohortSpec
  ): ZIO[CohortTable with Logging with S3 with StageConfiguration, Failure, Unit] =
    for {
      exclusionsSrc <- exclusionSourceLocation(cohortSpec)
      exclusionsManagedStream <- S3.getObject(exclusionsSrc)
      exclusions <- exclusionsManagedStream.use(parseExclusions)
      _ <- Logging.info(s"Loaded excluded subscriptions: $exclusions")
      importSrc <- importSourceLocation(cohortSpec)
      subscriptionIdsManagedStream <- S3.getObject(importSrc)
      count <- subscriptionIdsManagedStream.use(stream => writeSubscriptionIdsToCohortTable(stream, exclusions))
      _ <- Logging.info(s"Wrote $count subscription ids to the cohort table")
    } yield ()

  def parseExclusions(inputStream: InputStream): IO[SubscriptionIdUploadFailure, Set[String]] = {
    ZIO
      .effect(
        new CSVParser(new InputStreamReader(inputStream, "UTF-8"), csvFormat).getRecords.asScala
          .map(_.get(0))
          .toSet
      )
      .mapError { ex =>
        SubscriptionIdUploadFailure(s"Failed to read and parse the exclusions file: $ex")
      }
  }

  def writeSubscriptionIdsToCohortTable(
      inputStream: InputStream,
      exclusions: Set[String]
  ): ZIO[CohortTable with Logging, Failure, Long] = {
    ZStream
      .fromJavaIterator(
        new CSVParser(new InputStreamReader(inputStream, "UTF-8"), csvFormat).iterator()
      )
      .bimap(
        ex => SubscriptionIdUploadFailure(s"Failed to read subscription csv stream: $ex"),
        csvRecord => csvRecord.get(0)
      )
      .filterM { subscriptionId =>
        if (exclusions.contains(subscriptionId)) {
          Logging.info(s"Filtering subscription $subscriptionId as it is in the exclusion file") zipRight
            ZIO.succeed(false)
        } else
          ZIO.succeed(true)
      }
      .mapM { subscriptionId =>
        CohortTable
          .create(CohortItem(subscriptionId, ReadyForEstimation))
          .tap(_ => Logging.info(s"Imported subscription $subscriptionId"))
          .catchSome {
            case _: CohortItemAlreadyPresentFailure =>
              Logging.info(s"Ignored $subscriptionId as already in table") zipRight ZIO.succeed(())
          }
          .tapError(e => Logging.error(s"Subscription $subscriptionId failed: $e"))
      }
      .runCount
  }

  private def env(
      cohortSpec: CohortSpec
  ): ZLayer[Logging, Failure, CohortTable with S3 with StageConfiguration with Logging] =
    (LiveLayer.cohortTable(cohortSpec) and LiveLayer.s3 and EnvConfiguration.stageImp and LiveLayer.logging)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))

  def handle(input: CohortSpec): ZIO[ZEnv with Logging, Failure, HandlerOutput] =
    main(input).provideSomeLayer[ZEnv with Logging](env(input)).tapError(e => Logging.error(e.toString))
}
