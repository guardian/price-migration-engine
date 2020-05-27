package pricemigrationengine.handlers

import java.io.{InputStream, InputStreamReader}

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import org.apache.commons.csv.{CSVFormat, CSVParser}
import pricemigrationengine.model._
import pricemigrationengine.services.{EnvConfiguration, _}
import zio.console.Console
import zio.{App, IO, Runtime, ZEnv, ZIO, ZLayer, console}

import scala.jdk.CollectionConverters._

object SubscriptionIdUploadHandler extends App with RequestHandler[Unit, Unit] {
  val main = {
    for {
      config <- StageConfiguration.stageConfig
      exclusionsManaged <- S3ZIO.getObject(
        S3Location(
          s"price-migration-engine-${config.stage.toLowerCase}",
          "excluded-subscrition-ids.csv"
        )
      )
      exclusions <- exclusionsManaged.use(parseExclusions)
      _ <- Logging.info(s"Loaded excluded subscriptions: $exclusions")
    } yield ()
  }

  def parseExclusions(inputStream: InputStream): IO[SubscriptionIdUploadFailure, Set[String]] = {
    ZIO.effect(
      new CSVParser(new InputStreamReader(inputStream, "UTF-8"), CSVFormat.DEFAULT)
        .getRecords.asScala
        .map(_.get(0))
        .toSet
    ).mapError { ex =>
      SubscriptionIdUploadFailure(s"Failed to read and parse the exclusions file: $ex")
    }
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
