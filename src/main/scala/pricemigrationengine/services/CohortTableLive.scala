package pricemigrationengine.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util

import com.amazonaws.services.dynamodbv2.model.{AttributeAction, AttributeValue, AttributeValueUpdate, QueryRequest}
import pricemigrationengine.model.CohortTableFilter.EstimationComplete
import pricemigrationengine.model._
import pricemigrationengine.services.CohortTable.{Service}
import zio.stream.ZStream
import zio.{UIO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._


object CohortTableLive {
  private implicit val cohortItemDeserialiser: DynamoDBDeserialiser[CohortItem] =
    cohortItem =>
      getStringFromResults(cohortItem, "subscriptionNumber")
        .map(CohortItem.apply)

  private implicit val estimationResultSerialiser: DynamoDBUpdateSerialiser[EstimationResult] =
    estimationResult =>
      Map(
        stringFieldUpdate("processingStage", EstimationComplete.value),
        dateFieldUpdate("expectedStartDate", estimationResult.expectedStartDate),
        stringFieldUpdate("currency", estimationResult.currency),
        bigDecimalFieldUpdate("oldPrice", estimationResult.oldPrice),
        bigDecimalFieldUpdate("estimatedNewPrice", estimationResult.estimatedNewPrice)
      ).asJava

  private implicit val cohortTableKeySerialiser: DynamoDBSerialiser[CohortTableKey] =
    estimationResult =>
      Map(stringUpdate("subscriptionNumber", estimationResult.subscriptionNumber)).asJava

  private def stringFieldUpdate(fieldName: String, stringValue: String) =
    fieldName -> new AttributeValueUpdate(new AttributeValue().withS(stringValue), AttributeAction.PUT)

  private def dateFieldUpdate(fieldName: String, dateValue: LocalDate) =
    fieldName -> new AttributeValueUpdate(new AttributeValue().withS(dateValue.format(DateTimeFormatter.ISO_LOCAL_DATE)), AttributeAction.PUT)

  private def bigDecimalFieldUpdate(fieldName: String, value: BigDecimal) =
    fieldName -> new AttributeValueUpdate(new AttributeValue().withS(value.toString), AttributeAction.PUT)

  private def stringUpdate(fieldName: String, stringValue: String) =
    fieldName -> new AttributeValue().withS(stringValue)

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
                .withLimit(batchSize)
            ).map(_.mapError(error => CohortFetchFailure(error.toString)))
        }.provide(dynamoDBZStream)

        override def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] =
          for {
            fakeResult <- ZIO.effect(()).mapError(_ => CohortUpdateFailure(""))
          } yield fakeResult
      }
    }
}
