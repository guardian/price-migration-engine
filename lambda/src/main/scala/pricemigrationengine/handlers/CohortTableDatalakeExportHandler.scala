package pricemigrationengine.handlers

import java.io.{File, OutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import org.apache.commons.csv.{CSVFormat, CSVPrinter, QuoteMode}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio._
import zio.clock.Clock
import zio.stream.ZStream

import scala.util.Try

object CohortTableDatalakeExportHandler extends CohortHandler {
  private val csvFormat = CSVFormat.DEFAULT.withHeader("").withQuoteMode(QuoteMode.ALL)
  private val TempFileDirectory = "/tmp/"

  def main(
      cohortSpec: CohortSpec
  ): ZIO[Logging with CohortTable with S3 with StageConfiguration with Clock, Failure, HandlerOutput] =
    for {
      config <- StageConfiguration.stageConfig
      records <- CohortTable.fetchAll()
      s3Location = S3Location(
        s"price-migration-engine-${config.stage.toLowerCase}",
        s"data.csv"
      )
      _ <- writeCsvToS3(records, s3Location, cohortSpec, config)
    } yield HandlerOutput(isComplete = true)

  def writeCsvToS3(
      cohortItems: ZStream[Any, CohortFetchFailure, CohortItem],
      s3Location: S3Location,
      cohortSpec: CohortSpec,
      stageConfig: StageConfig
  ): ZIO[S3 with Logging, Failure, Unit] =
    localTempFile().use { filePath =>
      for {
        recordsWritten <- openOutputStream(filePath).use { tempFileOutputStream =>
          writeCsvToStream(cohortItems, tempFileOutputStream, cohortSpec, stageConfig)
        }

        putResult <-
          S3.putObject(s3Location, filePath.toFile)
            .mapError { failure =>
              CohortTableDatalakeExportFailure(s"Failed to write CohortItems to s3: ${failure.reason}")
            }

        _ <- Logging.info(
          s"Successfully wrote cohort table ${cohortSpec.tableName(stageConfig.stage)} containing $recordsWritten items " +
            s"to $s3Location: $putResult"
        )
      } yield ()
    }

  def localTempFile(): ZManaged[Any, CohortTableDatalakeExportFailure, Path] =
    ZManaged.make(
      ZIO
        .effect(Files.createTempFile(new File(TempFileDirectory).toPath, "CohortTableExport", "csv"))
        .mapError { throwable =>
          CohortTableDatalakeExportFailure(s"Failed to create temp file in $TempFileDirectory: ${throwable.getMessage}")
        }
    )(path => ZIO.effectTotal(Try(Files.delete(path))))

  def openOutputStream(path: Path): ZManaged[Any, CohortTableDatalakeExportFailure, OutputStream] =
    ZManaged.make(
      ZIO
        .effect(Files.newOutputStream(path))
        .mapError { throwable =>
          CohortTableDatalakeExportFailure(s"Failed to write to temp files $path: ${throwable.getMessage}")
        }
    )(stream => ZIO.effectTotal(stream.close()))

  def writeCsvToStream(
      cohortItems: ZStream[Any, Failure, CohortItem],
      outputStream: OutputStream,
      cohortSpec: CohortSpec,
      stageConfig: StageConfig
  ): IO[Failure, Long] = {
    managedCSVPrinter(
      outputStream,
      List(
        "cohortName",
        "cohortTable",
        "subscriptionName",
        "processingStage",
        "startDate",
        "currency",
        "oldPrice",
        "estimatedNewPrice",
        "billingPeriod",
        "whenEstimationDone",
        "salesforcePriceRiseId",
        "whenSfShowEstimate",
        "newPrice",
        "newSubscriptionId",
        "whenAmendmentDone",
        "whenNotificationSent",
        "whenNotificationSentWrittenToSalesforce",
        "whenAmendmentWrittenToSalesforce"
      )
    ).use { printer =>
      cohortItems.mapM { cohortItem =>
        ZIO
          .effect(
            printer.printRecord(
              cohortSpec.cohortName,
              cohortSpec.tableName(stageConfig.stage),
              cohortItem.subscriptionName,
              cohortItem.processingStage.value,
              cohortItem.startDate.getOrElse(""),
              cohortItem.currency.getOrElse(""),
              cohortItem.oldPrice.getOrElse(""),
              cohortItem.estimatedNewPrice.getOrElse(""),
              cohortItem.billingPeriod.getOrElse(""),
              cohortItem.whenEstimationDone.getOrElse(""),
              cohortItem.salesforcePriceRiseId.getOrElse(""),
              cohortItem.whenSfShowEstimate.getOrElse(""),
              cohortItem.newPrice.getOrElse(""),
              cohortItem.newSubscriptionId.getOrElse(""),
              cohortItem.whenAmendmentDone.getOrElse(""),
              cohortItem.whenNotificationSent.getOrElse(""),
              cohortItem.whenNotificationSentWrittenToSalesforce.getOrElse(""),
              cohortItem.whenAmendmentWrittenToSalesforce.getOrElse("")
            )
          )
          .mapError { ex =>
            CohortTableDatalakeExportFailure(s"Failed to write CohortItem as CSV to s3: ${ex.getMessage}")
          }
      }.runCount
    }
  }

  private def managedCSVPrinter(
      outputStream: OutputStream,
      headers: List[String]
  ): ZManaged[Any, CohortTableDatalakeExportFailure, CSVPrinter] = {
    ZManaged
      .makeEffect(
        new CSVPrinter(
          new OutputStreamWriter(outputStream, StandardCharsets.UTF_8.name()),
          csvFormat.withHeader(headers: _*)
        )
      )(printer => printer.close(true))
      .mapError { ex =>
        CohortTableDatalakeExportFailure(s"Failed to write CohortItems as CSV to s3: ${ex.getMessage}")
      }
  }

  private def env(
      cohortSpec: CohortSpec
  ): ZLayer[Logging, Failure, CohortTable with S3 with Logging with StageConfiguration] =
    (LiveLayer.cohortTable(cohortSpec) and LiveLayer.s3 and LiveLayer.logging and LiveLayer.stageConfig)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))

  def handle(input: CohortSpec): ZIO[ZEnv with Logging, Failure, HandlerOutput] =
    main(input).provideSomeLayer[ZEnv with Logging](env(input))
}
