package pricemigrationengine.handlers

import java.io.{BufferedWriter, OutputStream, OutputStreamWriter, PipedInputStream, PipedOutputStream}
import java.nio.charset.StandardCharsets

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import org.apache.commons.csv.{CSVFormat, CSVPrinter, QuoteMode}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.clock.Clock
import zio.console.Console
import zio.stream.ZStream
import zio.{App, ExitCode, IO, Runtime, ZEnv, ZIO, ZLayer, ZManaged, clock}

object CohortTableDatalakeExportHandler extends App with RequestHandler[Unit, Unit] {
  val csvFormat = CSVFormat.DEFAULT.withHeader("").withQuoteMode(QuoteMode.ALL)

  val main: ZIO[CohortTable with Logging with S3 with StageConfiguration with Clock, Failure, Unit] = {
    for {
      config <- StageConfiguration.stageConfig
      records <- CohortTable.fetchAll()
      today <- Time.today
      s3Location = S3Location(
        s"price-migration-engine-${config.stage.toLowerCase}",
        s"cohortTableExport-${today}.csv"
      )
      recordCount <- writeCsvToS3(records, s3Location)
      _ <- Logging
        .info(s"Wrote $recordCount CohortItems to s3: $s3Location")
    } yield ()
  }

  def writeCsvToS3(
    cohortItems: ZStream[Any, CohortFetchFailure, CohortItem],
    s3Location: S3Location
  ): ZIO[S3, Failure, Long] = {
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
      (_, count) = result
    } yield count
  }

  def writeCsvToStream(
    cohortItems: ZStream[Any, Failure, CohortItem],
    outputStream: OutputStream
  ): IO[Failure, Long] = {
    val managedCsvPrinter = ZManaged.makeEffect(
      new CSVPrinter(
        new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8.name())),
        csvFormat
      )
    )(printer => printer.close()).mapError { ex =>
      CohortTableDatalakeExportFailure(s"Failed to write CohortItems as CSV to s3: ${ex.getMessage}")
    }

    managedCsvPrinter.use { printer =>
     cohortItems
       .mapM { cohortItem =>
          ZIO
            .effect(printer.printRecord(""))
            .mapError { ex =>
              CohortTableDatalakeExportFailure(s"Failed to write CohortItem as CSV to s3: ${ex.getMessage}")
            }
        }
        .runCount
    }
  }

  private def env(loggingService: Logging.Service) = {
    val loggingLayer = ZLayer.succeed(loggingService)
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.stageImp ++ EnvConfiguration.cohortTableImp >>>
        CohortTableLive.impl ++ S3Live.impl ++ EnvConfiguration.stageImp ++ Clock.live
    (loggingLayer ++ cohortTableLayer)
      .tapError(e => loggingService.error(s"Failed to create service environment: $e"))
  }

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    main
      .provideSomeLayer(
        env(ConsoleLogging.service(Console.Service.live))
      )
      .exitCode

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.service(context))
      )
    )
}
