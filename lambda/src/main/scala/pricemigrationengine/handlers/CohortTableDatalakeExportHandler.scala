package pricemigrationengine.handlers

import org.apache.commons.csv.QuoteMode.ALL
import org.apache.commons.csv.{CSVFormat, CSVPrinter}
import pricemigrationengine.model._
import pricemigrationengine.services._
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import zio._
import zio.stream.ZStream

import java.io.{File, OutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.Try

object CohortTableDatalakeExportHandler extends CohortHandler {

  private val csvFormat = CSVFormat.Builder.create().setHeader("").setQuoteMode(ALL).build()
  private val TempFileDirectory = "/tmp/"

  private[handlers] def main(cohortSpec: CohortSpec) =
    ZIO.scoped(for {
      config <- ZIO.service[ExportConfig]
      records = CohortTable.fetchAll()
      filePath <- localTempFile()
      tempFileOutputStream <- openOutputStream(filePath)
      recordsWritten <- print(records, cohortSpec, tempFileOutputStream)
      s3Location = S3Location(config.exportBucketName, s"data/${cohortSpec.cohortName}.csv")
      putResult <- S3
        .putObject(s3Location, filePath.toFile, Some(ObjectCannedACL.BUCKET_OWNER_READ))
        .mapError(failure => CohortTableDatalakeExportFailure(s"Failed to write CohortItems to s3: ${failure.reason}"))
      _ <- Logging.info(
        s"Successfully wrote cohort '${cohortSpec.cohortName}' containing $recordsWritten items to $s3Location: $putResult"
      )
    } yield HandlerOutput(isComplete = true))

  private def localTempFile() =
    ZIO.acquireRelease(
      ZIO
        .attempt(Files.createTempFile(new File(TempFileDirectory).toPath, "CohortTableExport", "csv"))
        .mapError(throwable =>
          CohortTableDatalakeExportFailure(s"Failed to create temp file in $TempFileDirectory: ${throwable.getMessage}")
        )
    )(path => ZIO.succeed(Try(Files.delete(path))))

  private def openOutputStream(path: Path) =
    ZIO.fromAutoCloseable(
      ZIO
        .attempt(Files.newOutputStream(path))
        .mapError(throwable =>
          CohortTableDatalakeExportFailure(s"Failed to write to temp files $path: ${throwable.getMessage}")
        )
    )

  private def print(
      cohortStream: ZStream[CohortTable, CohortFetchFailure, CohortItem],
      cohortSpec: CohortSpec,
      outputStream: OutputStream
  ) =
    ZIO.scoped(for {
      printer <- buildPrinter(outputStream)
      count <- cohortStream.mapZIO(printItem(cohortSpec, printer)).runCount
    } yield count)

  private def printItem(cohortSpec: CohortSpec, printer: CSVPrinter)(cohortItem: CohortItem) =
    ZIO
      .attempt(
        printer.printRecord(
          cohortSpec.cohortName,
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
          cohortItem.whenAmendmentWrittenToSalesforce.getOrElse(""),
          cohortItem.cancellationReason.getOrElse("")
        )
      )
      .mapError(ex => CohortTableDatalakeExportFailure(s"Failed to write CohortItem as CSV to s3: ${ex.getMessage}"))

  private def buildPrinter(outputStream: OutputStream) =
    managedCSVPrinter(
      outputStream,
      List(
        "cohort_name",
        "subscription_name",
        "processing_stage",
        "start_date",
        "currency",
        "old_price",
        "estimated_new_price",
        "billing_period",
        "when_estimation_done",
        "salesforce_price_rise_id",
        "when_sf_show_estimate",
        "new_price",
        "new_subscription_id",
        "when_amendment_done",
        "when_notification_sent",
        "when_notification_sent_written_to_salesforce",
        "when_amendment_written_to_salesforce",
        "cancellation_reason"
      )
    )

  private def managedCSVPrinter(outputStream: OutputStream, headers: List[String]) =
    ZIO
      .acquireRelease(
        ZIO.attempt(
          new CSVPrinter(
            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8.name()),
            CSVFormat.Builder.create(csvFormat).setHeader(headers: _*).build()
          )
        )
      )(printer => ZIO.succeed(printer.close(true)))
      .mapError { ex =>
        CohortTableDatalakeExportFailure(s"Failed to write CohortItems as CSV to s3: ${ex.getMessage}")
      }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] =
    MigrationType(input) match {
      case SPV1V2E2025 => ZIO.succeed(HandlerOutput(isComplete = true))
      case _ =>
        main(input).provideSome[Logging](
          EnvConfig.cohortTable.layer,
          EnvConfig.stage.layer,
          EnvConfig.export.layer,
          DynamoDBClientLive.impl,
          DynamoDBZIOLive.impl,
          CohortTableLive.impl(input),
          S3Live.impl
        )
    }
}
