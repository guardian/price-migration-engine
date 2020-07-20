package pricemigrationengine.handlers

import java.io.{BufferedWriter, OutputStream, OutputStreamWriter, PipedInputStream, PipedOutputStream}
import java.nio.charset.StandardCharsets

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import org.apache.commons.csv.{CSVFormat, CSVPrinter, QuoteMode}
import pricemigrationengine.handlers.SalesforcePriceRiseCreationHandler.{env, main}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.clock.Clock
import zio.console.Console
import zio.stream.ZStream
import zio.{App, ExitCode, IO, Runtime, ZEnv, ZIO, ZLayer, ZManaged, clock}

object CohortTableDatalakeExportHandler extends CohortHandler {
  private val csvFormat = CSVFormat.DEFAULT.withHeader("").withQuoteMode(QuoteMode.ALL)

  def main(
    cohortSpec: CohortSpec
  ): ZIO[Logging with CohortTable with S3 with StageConfiguration with Clock, Failure, HandlerOutput] =
    for {
      config <- StageConfiguration.stageConfig
      records <- CohortTable.fetchAll()
      today <- Time.today
      s3Location = S3Location(
        s"price-migration-engine-${config.stage.toLowerCase}",
        s"cohortTableExport-$today.csv"
      )
      recordCount <- writeCsvToS3(records, s3Location)
      _ <- Logging
        .info(s"Wrote $recordCount CohortItems to s3: $s3Location")
    } yield HandlerOutput(isComplete = true)

  def writeCsvToS3(
    cohortItems: ZStream[Any, CohortFetchFailure, CohortItem],
    s3Location: S3Location
  ): ZIO[S3 with Logging, Failure, Long] =
    for {
      inputStream <- ZIO.effectTotal(new PipedInputStream())
      outputStream <-  ZIO.effectTotal(new PipedOutputStream(inputStream))
      result <- ZIO.tupledPar(
        S3.putObject(s3Location, inputStream)
          .mapError { failure =>
            CohortTableDatalakeExportFailure(s"Failed to write CohortItems to s3: ${failure.reason}")
          },
        writeCsvToStream(cohortItems, outputStream)
      )
      (putResult, count) = result
      _ <- Logging.info(s"Successfully wrote cohort table to $s3Location: $putResult")
    } yield count

  def writeCsvToStream(
    cohortItems: ZStream[Any, Failure, CohortItem],
    outputStream: OutputStream
  ): IO[Failure, Long] = {
    val managedCsvPrinter = ZManaged.makeEffect(
      new CSVPrinter(
        new OutputStreamWriter(outputStream, StandardCharsets.UTF_8.name()),
        csvFormat
      )
    )(printer => printer.close(true)).mapError { ex =>
      CohortTableDatalakeExportFailure(s"Failed to write CohortItems as CSV to s3: ${ex.getMessage}")
    }

    managedCsvPrinter.use { printer =>
     cohortItems
       .mapM { cohortItem =>
          ZIO
            .effect(printer.printRecord("abc"))
            .mapError { ex =>
              CohortTableDatalakeExportFailure(s"Failed to write CohortItem as CSV to s3: ${ex.getMessage}")
            }
        }
        .runCount
    }
  }

  private def env(
    cohortSpec: CohortSpec
  ): ZLayer[Logging, Failure, CohortTable with S3 with Logging with StageConfiguration] =
    (LiveLayer.cohortTable(cohortSpec.tableName) and LiveLayer.s3 and LiveLayer.logging and LiveLayer.stageConfig)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))

  def handle(input: CohortSpec): ZIO[ZEnv with Logging, Failure, HandlerOutput] =
    main(input).provideSomeLayer[ZEnv with Logging](env(input))
}
