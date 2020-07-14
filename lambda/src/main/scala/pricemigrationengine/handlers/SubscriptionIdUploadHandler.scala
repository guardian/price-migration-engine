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

  val main: ZIO[CohortTable with Logging with S3 with StageConfiguration, Failure, HandlerOutput] = {
    for {
      config <- StageConfiguration.stageConfig
      exclusionsManagedStream <- S3.getObject(
        S3Location(
          s"price-migration-engine-${config.stage.toLowerCase}",
          "excluded-subscription-ids.csv"
        )
      )
      exclusions <- exclusionsManagedStream.use(parseExclusions)
      _ <- Logging.info(s"Loaded excluded subscriptions: $exclusions")
      subscriptionIdsManagedStream <- S3.getObject(
        S3Location(
          s"price-migration-engine-${config.stage.toLowerCase}",
          "salesforce-subscription-id-report.csv"
        )
      )
      count <- subscriptionIdsManagedStream.use(stream => writeSubscriptionIdsToCohortTable(stream, exclusions))
      _ <- Logging.info(s"Wrote $count subscription ids to the cohort table")
    } yield HandlerOutput(isComplete = true)
  }

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
          for {
            _ <- Logging.info(s"Filtering subscription $subscriptionId as it is in the exclusion file")
          } yield false
        } else {
          ZIO.succeed(true)
        }
      }
      .tap { subcriptionId =>
        CohortTable.put(CohortItem(subcriptionId, ReadyForEstimation))
      }
      .runCount
  }

  private def env(
      cohortSpec: CohortSpec,
      loggingService: Logging.Service
  ): ZLayer[Any, Failure, Logging with CohortTable with S3 with StageConfiguration] = {
    val loggingLayer = ZLayer.succeed(loggingService)
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.stageImp ++ EnvConfiguration.cohortTableImp >>>
        CohortTableLive.impl(cohortSpec.tableName) ++ S3Live.impl ++ EnvConfiguration.stageImp
    (loggingLayer ++ cohortTableLayer)
      .tapError(e => loggingService.error(s"Failed to create service environment: $e"))
  }

  def handle(input: CohortSpec, loggingService: Logging.Service): ZIO[ZEnv, Failure, HandlerOutput] =
    main.provideCustomLayer(env(input, loggingService))
}
