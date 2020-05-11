package pricemigrationengine.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util

import com.amazonaws.services.dynamodbv2.model.{AttributeAction, AttributeValue, AttributeValueUpdate, QueryRequest}
import pricemigrationengine.dynamodb._
import pricemigrationengine.model.CohortTableFilter.EstimationComplete
import pricemigrationengine.model._
import zio.stream.ZStream
import zio.{IO, UIO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

case class CohortTableKey(subscriptionNumber: String)

object CohortTable {
  trait Service {
    def fetch(
      filter: CohortTableFilter,
      batchSize: Int
    ): UIO[ZStream[Any, CohortFetchFailure, CohortItem]]

    def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit]
  }

  def fetch(filter: CohortTableFilter, batchSize: Int): URIO[CohortTable, ZStream[Any, CohortFetchFailure, CohortItem]] =
    ZIO.accessM(_.get.fetch(filter, batchSize))

  def update(result: EstimationResult): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.accessM(_.get.update(result))

  private implicit val cohortItemDeserialiser: DynamoDBDeserialiser[CohortItem] =
    cohortItem =>
      getStringFromResults(cohortItem, "subscriptionNumber")
        .map(CohortItem.apply)

  private implicit val estimationResultSerialiser: DynamoDBUpdateSerialiser[EstimationResult] =
    estimationResult =>
      Map(
        stringFieldUpdate("processingStage", EstimationComplete.value),
        dateFieldUpdate("expectedStartDate", estimationResult.expectedStartDate),
        doubleFieldUpdate("estimatedNewPrice", estimationResult.estimatedNewPrice)
      ).asJava

  private implicit val cohortTableKeySerialiser: DynamoDBSerialiser[CohortTableKey] =
    estimationResult =>
      Map(stringUpdate("subscriptionNumber", estimationResult.subscriptionNumber)).asJava

  private def stringFieldUpdate(fieldName: String, stringValue: String) =
    fieldName -> new AttributeValueUpdate(new AttributeValue().withS(stringValue), AttributeAction.PUT)

  private def dateFieldUpdate(fieldName: String, dateValue: LocalDate) =
    fieldName -> new AttributeValueUpdate(new AttributeValue().withS(dateValue.format(DateTimeFormatter.ISO_LOCAL_DATE)), AttributeAction.PUT)

  private def doubleFieldUpdate(fieldName: String, doubleValue: Double) =
    fieldName -> new AttributeValueUpdate(new AttributeValue().withS(doubleValue.toString), AttributeAction.PUT)

  private def stringUpdate(fieldName: String, stringValue: String) =
    fieldName -> new AttributeValue().withS(stringValue)

  val impl: ZLayer[DynamoDBZIO, Nothing, UserRepo] =
    ZLayer.fromFunction { dynamoDBZIO: DynamoDBZIO =>
      new Service {
        override def fetch(filter: CohortTableFilter, batchSize: Int): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          UIO(
            dynamoDBZIO
              .get
              .query(
                new QueryRequest()
                  .withTableName("PriceMigrationEngineDev")
                  .withIndexName("ProcessingStageIndex")
                  .withKeyConditionExpression("processingStage = :processingStage")
                  .withExpressionAttributeValues(
                    Map(":processingStage" -> new AttributeValue(filter.value)).asJava
                  )
                  .withLimit(batchSize)
              ).mapError(error => CohortFetchFailure(error.toString))
          )
        }

        override def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] =
          dynamoDBZIO
            .get
            .update("PriceMigrationEngineDev", CohortTableKey(result.subscriptionName), result)
            .mapError(error => CohortUpdateFailure(error.toString))
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
