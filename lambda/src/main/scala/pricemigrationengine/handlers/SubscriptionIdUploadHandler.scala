package pricemigrationengine.handlers

import java.io.{InputStream, InputStreamReader}

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}
import pricemigrationengine.model._
import pricemigrationengine.services.{EnvConfiguration, _}
import zio.console.Console
import zio.stream.ZStream
import zio.{App, IO, Managed, Runtime, ZEnv, ZIO, ZLayer, ZManaged, console}

import scala.jdk.CollectionConverters._

object SubscriptionIdUploadHandler extends App with RequestHandler[Unit, Unit] {
  private val csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader()

  val main = {
    for {
      config <- StageConfiguration.stageConfig
      exclusionsManagedStream <- S3ZIO.getObject(
        S3Location(
          s"price-migration-engine-${config.stage.toLowerCase}",
          "excluded-subscrition-ids.csv"
        )
      )
      exclusions <- exclusionsManagedStream.use(parseExclusions)
      subscriptionIdsManagedStream <- S3ZIO.getObject(
        S3Location(
          s"price-migration-engine-${config.stage.toLowerCase}",
          "excluded-subscrition-ids.csv"
        )
      )
      _ <- subscriptionIdsManagedStream.use(stream => writeSubscrptionIdsToCohortTable(stream, exclusions))
      _ <- Logging.info(s"Loaded excluded subscriptions: $exclusions")
    } yield ()
  }

  def parseExclusions(inputStream: InputStream): IO[SubscriptionIdUploadFailure, Set[String]] = {
    ZIO.effect(
      new CSVParser(new InputStreamReader(inputStream, "UTF-8"), csvFormat)
        .getRecords.asScala
        .map(_.get(0))
        .toSet
    ).mapError { ex =>
      SubscriptionIdUploadFailure(s"Failed to read and parse the exclusions file: $ex")
    }
  }

  def writeSubscrptionIdsToCohortTable(inputStream: InputStream, exclusions: Set[String]): ZIO[CohortTable with Logging, Failure, Unit] = {
      ZStream.fromJavaIterator(
        new CSVParser(new InputStreamReader(inputStream, "UTF-8"), csvFormat).iterator()
      )
      .mapError { ex =>
        SubscriptionIdUploadFailure(s"Failed to read subscription csv stream: $ex")
      }
      .map { csvRecord =>
        csvRecord.get(0)
      }
      .filterNot(subscriptionId => exclusions.contains(subscriptionId))
      .foreach { subcriptionId =>
        writeSubscriptionIdToCohortTable(subcriptionId)
      }
  }

  def writeSubscriptionIdToCohortTable(
    subcriptionId: ZuoraSubscriptionId
  ): ZIO[CohortTable with Logging, CohortUpdateFailure, Unit] = {
    for {
      _ <- Logging.info(s"Writing subscription $subcriptionId to ChortTable")
      _ <- CohortTable.put(Subscription(subcriptionId))
    } yield ()
  }

  private def env(loggingLayer: ZLayer[Any, Nothing, Logging]) = {
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.stageImp ++ EnvConfiguration.cohortTableImp >>>
        CohortTableLive.impl ++ S3ZIOLive.impl ++ EnvConfiguration.stageImp
    loggingLayer ++ cohortTableLayer
  }

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideSomeLayer(env(Console.live >>> ConsoleLogging.impl))
      .foldM(
        e => console.putStrLn(s"Failed: $e") *> ZIO.succeed(1),
        _ => console.putStrLn("Succeeded!") *> ZIO.succeed(0)
      )

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.impl(context))
      )
    )
}
