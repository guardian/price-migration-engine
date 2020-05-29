package pricemigrationengine.services

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util

import com.amazonaws.services.dynamodbv2.model.{AttributeAction, AttributeValue, AttributeValueUpdate, QueryRequest}
import pricemigrationengine.model.CohortTableFilter.{AmendmentComplete, EstimationComplete, ReadyForEstimation, SalesforcePriceRiceCreationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services.CohortTable.Service
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

object CohortTableLive {
  private implicit val cohortItemDeserialiser: DynamoDBDeserialiser[CohortItem] =
    cohortItem =>
      for {
        subscriptionNumber <- getStringFromResults(cohortItem, "subscriptionNumber")
        processingStage <- getCohortTableFilter(cohortItem, "processingStage")
        expectedStartDate <- getOptionalDateFromResults(cohortItem, "expectedStartDate")
        currency <- getOptionalStringFromResults(cohortItem, "currency")
        oldPrice <-  getOptionalBigDecimalFromResults(cohortItem, "oldPrice")
        estimatedNewPrice <- getOptionalBigDecimalFromResults(cohortItem, "estimatedNewPrice")
        billingPeriod <- getOptionalStringFromResults(cohortItem, "currency")
      } yield CohortItem(
        subscriptionNumber,
        processingStage,
        expectedStartDate,
        currency,
        oldPrice,
        estimatedNewPrice,
        billingPeriod
      )

  private implicit val cohortItemUpdateSerialiser: DynamoDBUpdateSerialiser[CohortItem] =
    cohortItem =>
      List(
        Option(stringFieldUpdate("processingStage", cohortItem.processingStage.value)),
        cohortItem.expectedStartDate
          .map(expectedStartDate => dateFieldUpdate("expectedStartDate", expectedStartDate)),
        cohortItem.currency
          .map(currency => stringFieldUpdate("currency", currency)),
        cohortItem.oldPrice
          .map(oldPrice => bigDecimalFieldUpdate("oldPrice", oldPrice)),
        cohortItem.estimatedNewPrice
          .map(estimatedNewPrice => bigDecimalFieldUpdate("estimatedNewPrice", estimatedNewPrice)),
        cohortItem.billingPeriod.map(billingPeriod =>
          stringFieldUpdate("billingPeriod", billingPeriod)),
        cohortItem.whenEstimationDone
          .map(whenEstimationDone => instantFieldUpdate("whenEstimationDone", whenEstimationDone)),
        cohortItem.salesforcePriceRiseId
        .map(salesforcePriceRiseId => stringFieldUpdate("salesforcePriceRiseId", salesforcePriceRiseId)),
        cohortItem.whenSfShowEstimate
          .map(whenSfShowEstimate => instantFieldUpdate("whenSfShowEstimate", whenSfShowEstimate)),
        cohortItem.startDate
          .map(startDate => dateFieldUpdate("startDate", startDate)),
        cohortItem.newPrice
          .map(newPrice => bigDecimalFieldUpdate("newPrice", newPrice)),
        cohortItem.newSubscriptionId
          .map(newSubscriptionId => stringFieldUpdate("newSubscriptionId", newSubscriptionId)),
        cohortItem.whenAmendmentDone
          .map(whenAmendmentDone => instantFieldUpdate("whenAmendmentDone", whenAmendmentDone)),
      ).flatten.toMap.asJava

  private implicit val estimationResultSerialiser: DynamoDBUpdateSerialiser[EstimationResult] =
    estimationResult =>
      Map(
        stringFieldUpdate("processingStage", EstimationComplete.value),
        dateFieldUpdate("expectedStartDate", estimationResult.expectedStartDate),
        stringFieldUpdate("currency", estimationResult.currency),
        bigDecimalFieldUpdate("oldPrice", estimationResult.oldPrice),
        bigDecimalFieldUpdate("estimatedNewPrice", estimationResult.estimatedNewPrice),
        stringFieldUpdate("billingPeriod", estimationResult.billingPeriod),
        stringFieldUpdate("whenEstimationDone", Instant.now.toString)
      ).asJava

  private implicit val amendmentResultSerialiser: DynamoDBUpdateSerialiser[AmendmentResult] =
    amendmentResult =>
      Map(
        stringFieldUpdate("processingStage", AmendmentComplete.value),
        dateFieldUpdate("startDate", amendmentResult.startDate),
        bigDecimalFieldUpdate("newPrice", amendmentResult.newPrice),
        stringFieldUpdate("newSubscriptionId", amendmentResult.newSubscriptionId),
        stringFieldUpdate("whenAmendmentDone", Instant.now.toString)
      ).asJava

  private implicit val salesforcePriceRiseCreationResultSerialiser: DynamoDBUpdateSerialiser[SalesforcePriceRiseCreationDetails] =
    salesforcePriceRise =>
      Map(
        stringFieldUpdate("processingStage", SalesforcePriceRiceCreationComplete.value),
        stringFieldUpdate("salesforcePriceRiseId", salesforcePriceRise.id),
        instantFieldUpdate("whenSfShowEstimate", salesforcePriceRise.whenSfShowEstimate)
      ).asJava

  private implicit val cohortTableKeySerialiser: DynamoDBSerialiser[CohortTableKey] =
    key => Map(stringUpdate("subscriptionNumber", key.subscriptionNumber)).asJava

  private implicit val subscriptionSerialiser: DynamoDBSerialiser[CohortItem] =
    cohortItem => Map(
      stringUpdate("subscriptionNumber", cohortItem.subscriptionName),
      stringUpdate("processingStage", ReadyForEstimation.value)
    ).asJava

  private def stringFieldUpdate(fieldName: String, stringValue: String) =
    fieldName -> new AttributeValueUpdate(new AttributeValue().withS(stringValue), AttributeAction.PUT)

  private def dateFieldUpdate(fieldName: String, dateValue: LocalDate) =
    fieldName -> new AttributeValueUpdate(
      new AttributeValue().withS(dateValue.format(DateTimeFormatter.ISO_LOCAL_DATE)),
      AttributeAction.PUT
    )

  private def instantFieldUpdate(fieldName: String, instant: Instant) =
    fieldName -> new AttributeValueUpdate(
      new AttributeValue().withS(DateTimeFormatter.ISO_DATE_TIME.format(instant.atZone(ZoneOffset.UTC))),
      AttributeAction.PUT
    )

  private def bigDecimalFieldUpdate(fieldName: String, value: BigDecimal) =
    fieldName -> new AttributeValueUpdate(new AttributeValue().withN(value.toString), AttributeAction.PUT)

  private def stringUpdate(fieldName: String, stringValue: String) =
    fieldName -> new AttributeValue().withS(stringValue)

  private def getStringFromResults(result: util.Map[String, AttributeValue], fieldName: String) = {
    for {
      optionalString <- getOptionalStringFromResults(result, fieldName)
      string <- ZIO
        .fromOption(optionalString)
        .orElseFail(DynamoDBZIOError(s"The '$fieldName' field did not exist in the record '$result''"))
    } yield string
  }

  private def getCohortTableFilter(result: util.Map[String, AttributeValue], fieldName: String) = {
    for {
      string <- getStringFromResults(result, fieldName)
      string <- ZIO
        .fromOption(CohortTableFilter.all.find(_.value == string))
        .orElseFail(DynamoDBZIOError(s"The '$fieldName' contained an invalid CohortTableFilter '$string'"))
    } yield string
  }

  private def getOptionalStringFromResults(result: util.Map[String, AttributeValue], fieldName: String): IO[DynamoDBZIOError, Option[String]] = {
    result
      .asScala
      .get(fieldName)
      .fold[IO[DynamoDBZIOError, Option[String]]](ZIO.succeed(None)) { attributeValue =>
        ZIO
          .fromOption(Option(attributeValue.getS))
          .orElseFail(DynamoDBZIOError(s"The '$fieldName' field was not a string in the record '$result'"))
          .map(Some.apply)
      }
  }

  private def getOptionalNumberStringFromResults(result: util.Map[String, AttributeValue], fieldName: String): IO[DynamoDBZIOError, Option[String]] = {
    result
      .asScala
      .get(fieldName)
      .fold[IO[DynamoDBZIOError, Option[String]]](ZIO.succeed(None)) { attributeValue =>
        ZIO
          .fromOption(Option(attributeValue.getN))
          .orElseFail(DynamoDBZIOError(s"The '$fieldName' field was not a number in the record '$result'"))
          .map(Some.apply)
      }
  }

  private def getOptionalDateFromResults(result: util.Map[String, AttributeValue], fieldName: String): IO[DynamoDBZIOError, Option[LocalDate]] =
    for {
      optionalString <- getOptionalStringFromResults(result, fieldName)
      optionalDate <-
        optionalString.fold[IO[DynamoDBZIOError, Option[LocalDate]]](ZIO.succeed(None)) { string =>
          ZIO
            .effect(Some(LocalDate.parse(string)))
            .mapError(ex => DynamoDBZIOError(s"The '$fieldName' has value '$string' which is not a valid date yyyy-MM-dd"))
        }
    } yield optionalDate

  private def getOptionalBigDecimalFromResults(result: util.Map[String, AttributeValue], fieldName: String): IO[DynamoDBZIOError, Option[BigDecimal]] =
    for {
      optionalNumberString <- getOptionalNumberStringFromResults(result, fieldName)
      optionalDecimal <-
        optionalNumberString.fold[IO[DynamoDBZIOError, Option[BigDecimal]]](ZIO.succeed(None)) { string =>
          ZIO
            .effect(Some(BigDecimal(string)))
            .mapError(ex => DynamoDBZIOError(s"The '$fieldName' has value '$string' which is not a valid number"))
        }
    } yield optionalDecimal

  val impl: ZLayer[DynamoDBZIO with StageConfiguration with CohortTableConfiguration with Logging, Nothing, CohortTable] =
    ZLayer.fromFunction { dependencies: DynamoDBZIO with StageConfiguration with CohortTableConfiguration with Logging =>
      new Service {
        override def fetch(
            filter: CohortTableFilter
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          for {
            cohortTableConfig <- CohortTableConfiguration.cohortTableConfig
              .mapError(error => CohortFetchFailure(s"Failed to get configuration:${error.reason}"))
            stageConfig <- StageConfiguration.stageConfig
              .mapError(error => CohortFetchFailure(s"Failed to get configuration:${error.reason}"))
            queryResults <- DynamoDBZIO
              .query(
                new QueryRequest()
                  .withTableName(s"PriceMigrationEngine${stageConfig.stage}")
                  .withIndexName("ProcessingStageIndexV2")
                  .withKeyConditionExpression("processingStage = :processingStage")
                  .withExpressionAttributeValues(
                    Map(":processingStage" -> new AttributeValue(filter.value)).asJava
                  )
                  .withLimit(cohortTableConfig.batchSize)
              )
              .map(_.mapError(error => CohortFetchFailure(error.toString)))
          } yield queryResults
        }.provide(dependencies)

        override def put(cohortItem: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
          for {
            config <- StageConfiguration.stageConfig
              .mapError(error => CohortUpdateFailure(s"Failed to get configuration:${error.reason}"))
            result <- DynamoDBZIO
              .put(s"PriceMigrationEngine${config.stage}", cohortItem)
              .mapError(error => CohortUpdateFailure(error.toString))
          } yield result
        }.provide(dependencies)

        override def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] = {
          for {
            config <- StageConfiguration.stageConfig
              .mapError(error => CohortUpdateFailure(s"Failed to get configuration:${error.reason}"))
            result <- DynamoDBZIO
              .update(s"PriceMigrationEngine${config.stage}", CohortTableKey(result.subscriptionName), result)
              .mapError(error => CohortUpdateFailure(error.toString))
          } yield result
        }.provide(dependencies)

        override def update(subscriptionName: String, result: SalesforcePriceRiseCreationDetails): ZIO[Any, CohortUpdateFailure, Unit] = {
          for {
            config <- StageConfiguration.stageConfig
              .mapError(error => CohortUpdateFailure(s"Failed to get configuration:${error.reason}"))
            result <- DynamoDBZIO
              .update(s"PriceMigrationEngine${config.stage}", CohortTableKey(subscriptionName), result)
              .mapError(error => CohortUpdateFailure(error.toString))
          } yield result
        }.provide(dependencies)

        override def update(result: AmendmentResult): ZIO[Any, CohortUpdateFailure, Unit] = {
          (for {
            config <- StageConfiguration.stageConfig
              .mapError(error => CohortUpdateFailure(s"Failed to get configuration:${error.reason}"))
            result <- DynamoDBZIO
              .update(s"PriceMigrationEngine${config.stage}", CohortTableKey(result.subscriptionName), result)
              .mapError(error => CohortUpdateFailure(error.toString))
          } yield result)
            .tapBoth(
              e => Logging.error(s"Failed to update Cohort table: $e"),
              _ => Logging.info(s"Wrote $result to Cohort table")
            )
        }.provide(dependencies)

        override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
          (for {
            config <- StageConfiguration.stageConfig
              .mapError(error => CohortUpdateFailure(s"Failed to get configuration:${error.reason}"))
            result <- DynamoDBZIO
              .update(s"PriceMigrationEngine${config.stage}", CohortTableKey(result.subscriptionName), result)
              .mapError(error => CohortUpdateFailure(error.toString))
          } yield result)
            .tapBoth(
              e => Logging.error(s"Failed to update Cohort table: $e"),
              _ => Logging.info(s"Wrote $result to Cohort table")
            )
        }.provide(dependencies)
      }
    }
}
