package pricemigrationengine.services

import java.util

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest}
import pricemigrationengine.dynamodb.DynamoDBZIO
import pricemigrationengine.model._
import zio.stream.ZStream
import zio.{ZIO, ZLayer}

import scala.jdk.CollectionConverters._


object CohortTable {

  trait Service {
    def fetch(filter: CohortTableFilter, batchSize: Int): ZStream[Any, CohortFetchFailure, CohortItem]

    def update(result: ResultOfEstimation): ZIO[Any, CohortUpdateFailure, Unit]
  }

  def fetch(filter: CohortTableFilter, batchSize: Int): ZIO[CohortTable, CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] =
    ZIO.access(_.get.fetch(filter, batchSize))

  def update(result: ResultOfEstimation): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.accessM(_.get.update(result))

  val impl: ZLayer[DynamoDBZIO, Nothing, UserRepo] =
    ZLayer.fromFunction { dynamoDB: DynamoDBZIO =>
      new Service {
        override def fetch(filter: CohortTableFilter, batchSize: Int): ZStream[Any, CohortFetchFailure, CohortItem] = {
          val queryRequest = new QueryRequest()
            .withTableName("PriceMigrationEngineDEV")
            .withKeyConditionExpression("ProcessingStageIndex = :processingStage")
            .withExpressionAttributeValues(
              Map("processingStage" -> new AttributeValue(filter.value)).asJava
            )

          dynamoDB.get.query(
            queryRequest,
            { result =>
              val fieldName = "subscriptionName"
              getStringFromResults(result, fieldName).map(CohortItem.apply)
            }
          )
          .mapError(CohortFetchFailure.apply)
        }

        override def update(result: ResultOfEstimation): ZIO[Any, CohortUpdateFailure, Unit] = ???
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
