package pricemigrationengine.services

import java.util

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest}
import pricemigrationengine.model._
import pricemigrationengine.services.CohortTable.Service
import zio.stream.ZStream
import zio.{UIO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._


object CohortTableLive {
  val impl: ZLayer[DynamoDBZIO, Nothing, CohortTable] =
    ZLayer.fromFunction { dynamoDBZStream: DynamoDBZIO =>
      new Service {
        override def fetch(filter: CohortTableFilter, batchSize: Int): UIO[ZStream[Any, CohortFetchFailure, CohortItem]] = {
            DynamoDBZIO.query(
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
            ).map(_.mapError(error => CohortFetchFailure(error.toString)))
        }.provide(dynamoDBZStream)

        override def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] =
          for {
            fakeResult <- ZIO.effect(()).mapError(_ => CohortUpdateFailure(""))
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
