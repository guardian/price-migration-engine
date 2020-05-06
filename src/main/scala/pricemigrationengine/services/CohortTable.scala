package pricemigrationengine.services

import java.util

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest}
import pricemigrationengine.dynamodb.DynamoDBZIO
import pricemigrationengine.model._
import zio.console.Console
import zio.stream.ZStream
import zio.{ZIO, ZLayer}

import scala.jdk.CollectionConverters._


object CohortTable {

  trait Service {
    def fetch(
      filter: CohortTableFilter,
      batchSize: Int
    ): ZIO[Any, CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]]

    def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit]
  }

  def fetch(filter: CohortTableFilter, batchSize: Int): ZIO[CohortTable, CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] =
    ZIO.accessM(_.get.fetch(filter, batchSize))

  def update(result: EstimationResult): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.accessM(_.get.update(result))

  val impl: ZLayer[DynamoDBZIO with Console, Nothing, UserRepo] =
    ZLayer.fromFunction { deps: Console with DynamoDBZIO  =>
      new Service {
        override def fetch(filter: CohortTableFilter, batchSize: Int): ZIO[Any, Nothing, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          for {
            _ <- deps.get[Console.Service].putStrLn(s"Getting values from CohortTable for filter $filter")
          } yield deps.get[DynamoDBZIO.Service].query(
              new QueryRequest()
                .withTableName("PriceMigrationEngineDEV")
                .withKeyConditionExpression("ProcessingStageIndex = :processingStage")
                .withExpressionAttributeValues(
                  Map("processingStage" -> new AttributeValue(filter.value)).asJava
                ),
              { result =>
                val fieldName = "subscriptionName"
                getStringFromResults(result, fieldName).map(CohortItem.apply)
              }
            )
            .mapError(CohortFetchFailure.apply)
        }

        override def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] = ???
      }
    }

  private def getStringFromResults(result: util.Map[String, AttributeValue], fieldName: String) = {
    ZIO
      .fromOption(result.asScala.get(fieldName))
      .mapError(_ => s"The '$fieldName' field did not exist in the record $result")
      .flatMap { attributeValue =>
        ZIO
          .fromOption(Option(attributeValue.getS))
          .mapError(_ => s"The '$fieldName' field was not a string in the record $result")
      }
  }
}
