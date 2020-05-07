package pricemigrationengine.services

import java.util

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest}
import pricemigrationengine.dynamodb.{DynamoDBZIO, DynamoDBZIOError}
import pricemigrationengine.model._
import zio.console.Console
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._


object CohortTable {

  trait Service {
    def fetch(
      filter: CohortTableFilter,
      batchSize: Int
    ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]]

    def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit]
  }

  def fetch(filter: CohortTableFilter, batchSize: Int): ZIO[CohortTable, CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] =
    ZIO.accessM(_.get.fetch(filter, batchSize))

  def update(result: EstimationResult): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.accessM(_.get.update(result))

  val impl: ZLayer[DynamoDBZIO with Console, Nothing, UserRepo] =
    ZLayer.fromFunction { deps: Console with DynamoDBZIO =>
      val dynamoDBZIO = deps.get[DynamoDBZIO.Service]
      val console: Console.Service = deps.get[Console.Service]

      new Service {

        override def fetch(filter: CohortTableFilter, batchSize: Int): ZIO[Any, Nothing, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          for {
            _ <- console.putStrLn(s"Getting values from CohortTable for filter $filter")
            stream <- dynamoDBZIO.query(
              new QueryRequest()
                .withTableName("PriceMigrationEngineDev")
                .withIndexName("ProcessingStageIndex")
                .withKeyConditionExpression("processingStage = :processingStage")
                .withExpressionAttributeValues(
                  Map(":processingStage" -> new AttributeValue(filter.value)).asJava
                )
                .withLimit(batchSize),
              { result =>
                getStringFromResults(result, "subscriptionNumber").map(CohortItem.apply)
              }
            )
          } yield stream.mapError(error => CohortFetchFailure(error.toString))
        }

        override def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] =
          for {
            fakeResult <- ZIO.effect(()).mapError(_ => CohortUpdateFailure(""))
            _ <- console.putStrLn(s"Saving ${result}")
          } yield fakeResult
      }
    }

  private def getStringFromResults(result: util.Map[String, AttributeValue], fieldName: String) = {
    ZIO
      .fromOption(result.asScala.get(fieldName))
      .mapError(_ => DynamoDBZIOError(s"The '$fieldName' field did not exist in the record $result"))
      .flatMap { attributeValue =>
        ZIO
          .fromOption(Option(attributeValue.getS))
          .mapError(_ => DynamoDBZIOError(s"The '$fieldName' field was not a string in the record $result"))
      }
  }
}
