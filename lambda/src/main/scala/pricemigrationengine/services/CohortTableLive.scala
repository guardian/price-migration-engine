package pricemigrationengine.services

import pricemigrationengine.model._
import pricemigrationengine.model.dynamodb.Conversions._
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, QueryRequest, ScanRequest}
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer}

import java.time.LocalDate
import scala.jdk.CollectionConverters._

object CohortTableLive {

  private val keyAttribName = "subscriptionNumber"

  private val ProcessingStageIndexName = "ProcessingStageIndexV2"
  private val ProcessingStageAndStartDateIndexName = "ProcessingStageStartDateIndexV1"

  private implicit val cohortItemDeserialiser: DynamoDBDeserialiser[CohortItem] = { cohortItem =>
    ZIO
      .fromEither(
        for {
          subscriptionNumber <- getStringFromResults(cohortItem, keyAttribName)
          processingStage <- getCohortTableFilter(cohortItem, "processingStage")
          startDate <- getOptionalDateFromResults(cohortItem, "startDate")
          currency <- getOptionalStringFromResults(cohortItem, "currency")
          oldPrice <- getOptionalBigDecimalFromResults(cohortItem, "oldPrice")
          estimatedNewPrice <- getOptionalBigDecimalFromResults(cohortItem, "estimatedNewPrice")
          billingPeriod <- getOptionalStringFromResults(cohortItem, "billingPeriod")
          whenEstimationDone <- getOptionalInstantFromResults(cohortItem, "whenEstimationDone")
          salesforcePriceRiseId <- getOptionalStringFromResults(cohortItem, "salesforcePriceRiseId")
          whenSfShowEstimate <- getOptionalInstantFromResults(cohortItem, "whenSfShowEstimate")
          newPrice <- getOptionalBigDecimalFromResults(cohortItem, "newPrice")
          newSubscriptionId <- getOptionalStringFromResults(cohortItem, "newSubscriptionId")
          whenAmendmentDone <- getOptionalInstantFromResults(cohortItem, "whenAmendmentDone")
          whenNotificationSent <- getOptionalInstantFromResults(cohortItem, "whenNotificationSent")
          whenNotificationSentWrittenToSalesforce <-
            getOptionalInstantFromResults(cohortItem, "whenNotificationSentWrittenToSalesforce")
          cancellationReason <-
            getOptionalStringFromResults(cohortItem, "cancellationReason")
          doNotProcessUntil <- getOptionalDateFromResults(cohortItem, "doNotProcessUntil")
          migrationExtraAttributes <- getOptionalStringFromResults(cohortItem, "migrationExtraAttributes")
          extendedAttribute1 <- getOptionalStringFromResults(cohortItem, "extendedAttribute1")
          extendedAttribute2 <- getOptionalStringFromResults(cohortItem, "extendedAttribute2")
          extendedAttribute3 <- getOptionalStringFromResults(cohortItem, "extendedAttribute3")
          extendedAttribute4 <- getOptionalStringFromResults(cohortItem, "extendedAttribute4")
          extendedAttribute5 <- getOptionalStringFromResults(cohortItem, "extendedAttribute5")
          extendedAttribute6 <- getOptionalStringFromResults(cohortItem, "extendedAttribute6")
        } yield CohortItem(
          subscriptionName = subscriptionNumber,
          processingStage = processingStage,
          startDate = startDate,
          currency = currency,
          oldPrice = oldPrice,
          estimatedNewPrice = estimatedNewPrice,
          billingPeriod = billingPeriod,
          whenEstimationDone = whenEstimationDone,
          salesforcePriceRiseId = salesforcePriceRiseId,
          whenSfShowEstimate = whenSfShowEstimate,
          newPrice = newPrice,
          newSubscriptionId = newSubscriptionId,
          whenAmendmentDone = whenAmendmentDone,
          whenNotificationSent = whenNotificationSent,
          whenNotificationSentWrittenToSalesforce = whenNotificationSentWrittenToSalesforce,
          cancellationReason = cancellationReason,
          doNotProcessUntil = doNotProcessUntil,
          migrationExtraAttributes = migrationExtraAttributes,
          extendedAttribute1 = extendedAttribute1,
          extendedAttribute2 = extendedAttribute2,
          extendedAttribute3 = extendedAttribute3,
          extendedAttribute4 = extendedAttribute4,
          extendedAttribute5 = extendedAttribute5,
          extendedAttribute6 = extendedAttribute6,
        )
      )
      .mapError(e => DynamoDBZIOError(e))
  }

  private implicit val cohortItemUpdateSerialiser: DynamoDBUpdateSerialiser[CohortItem] =
    cohortItem =>
      List(
        Option(stringFieldUpdate("processingStage", cohortItem.processingStage.value)),
        cohortItem.startDate.map(startDate => dateFieldUpdate("startDate", startDate)),
        cohortItem.currency.map(currency => stringFieldUpdate("currency", currency)),
        cohortItem.oldPrice.map(oldPrice => bigDecimalFieldUpdate("oldPrice", oldPrice)),
        cohortItem.estimatedNewPrice
          .map(estimatedNewPrice => bigDecimalFieldUpdate("estimatedNewPrice", estimatedNewPrice)),
        cohortItem.billingPeriod.map(billingPeriod => stringFieldUpdate("billingPeriod", billingPeriod)),
        cohortItem.whenEstimationDone
          .map(whenEstimationDone => instantFieldUpdate("whenEstimationDone", whenEstimationDone)),
        cohortItem.salesforcePriceRiseId
          .map(salesforcePriceRiseId => stringFieldUpdate("salesforcePriceRiseId", salesforcePriceRiseId)),
        cohortItem.whenSfShowEstimate
          .map(whenSfShowEstimate => instantFieldUpdate("whenSfShowEstimate", whenSfShowEstimate)),
        cohortItem.startDate.map(startDate => dateFieldUpdate("startDate", startDate)),
        cohortItem.newPrice.map(newPrice => bigDecimalFieldUpdate("newPrice", newPrice)),
        cohortItem.newSubscriptionId
          .map(newSubscriptionId => stringFieldUpdate("newSubscriptionId", newSubscriptionId)),
        cohortItem.whenAmendmentDone
          .map(whenAmendmentDone => instantFieldUpdate("whenAmendmentDone", whenAmendmentDone)),
        cohortItem.whenNotificationSent
          .map(whenNotificationSent => instantFieldUpdate("whenNotificationSent", whenNotificationSent)),
        cohortItem.whenNotificationSentWrittenToSalesforce
          .map(whenNotificationSentWrittenToSalesforce =>
            instantFieldUpdate(
              "whenNotificationSentWrittenToSalesforce",
              whenNotificationSentWrittenToSalesforce
            )
          ),
        cohortItem.whenAmendmentWrittenToSalesforce.map(instant =>
          instantFieldUpdate("whenAmendmentWrittenToSalesforce", instant)
        ),
        cohortItem.cancellationReason.map(reason => stringFieldUpdate("cancellationReason", reason)),
        cohortItem.doNotProcessUntil.map(date => dateFieldUpdate("doNotProcessUntil", date)),
        cohortItem.migrationExtraAttributes.map(extra => stringFieldUpdate("migrationExtraAttributes", extra)),
        cohortItem.extendedAttribute1.map(extra => stringFieldUpdate("extendedAttribute1", extra)),
        cohortItem.extendedAttribute2.map(extra => stringFieldUpdate("extendedAttribute2", extra)),
        cohortItem.extendedAttribute3.map(extra => stringFieldUpdate("extendedAttribute3", extra)),
        cohortItem.extendedAttribute4.map(extra => stringFieldUpdate("extendedAttribute4", extra)),
        cohortItem.extendedAttribute5.map(extra => stringFieldUpdate("extendedAttribute5", extra)),
        cohortItem.extendedAttribute6.map(extra => stringFieldUpdate("extendedAttribute6", extra)),
      ).flatten.toMap.asJava

  private implicit val cohortTableKeySerialiser: DynamoDBSerialiser[CohortTableKey] =
    key => Map(stringUpdate(keyAttribName, key.subscriptionNumber)).asJava

  private implicit val cohortTableSerialiser: DynamoDBSerialiser[CohortItem] =
    cohortItem =>
      Map(
        stringUpdate(keyAttribName, cohortItem.subscriptionName),
        stringUpdate("processingStage", cohortItem.processingStage.value)
      ).asJava

  def impl(
      cohortSpec: CohortSpec
  ): ZLayer[DynamoDBZIO with StageConfig with CohortTableConfig with Logging, ConfigFailure, CohortTable] = {
    ZLayer.fromZIO {
      for {
        dynamoDbZio <- ZIO.service[DynamoDBZIO]
        stageConfig <- ZIO.service[StageConfig]
        tableName = cohortSpec.tableName(stageConfig.stage)
        cohortTableConfig <- ZIO.service[CohortTableConfig]
        logging <- ZIO.service[Logging]
      } yield new CohortTable {

        override def fetch(
            filter: CohortTableFilter,
            latestStartDateInclusive: Option[LocalDate]
        ): ZStream[Any, CohortFetchFailure, CohortItem] = {
          val indexName =
            latestStartDateInclusive
              .fold(ProcessingStageIndexName)(_ => ProcessingStageAndStartDateIndexName)
          val queryRequest =
            QueryRequest.builder
              .tableName(tableName)
              .indexName(indexName)
              .keyConditionExpression(
                "processingStage = :processingStage" + latestStartDateInclusive.fold("") { _ =>
                  " AND startDate <= :latestStartDateInclusive"
                }
              )
              .expressionAttributeValues(
                List(
                  Some(":processingStage" -> AttributeValue.builder.s(filter.value).build()),
                  latestStartDateInclusive.map { latestStartDateInclusive =>
                    ":latestStartDateInclusive" -> AttributeValue.builder
                      .s(latestStartDateInclusive.toString)
                      .build()
                  }
                ).flatten.toMap.asJava
              )
              .limit(cohortTableConfig.batchSize)
              .build()
          dynamoDbZio.query(queryRequest).mapError(error => CohortFetchFailure(error.toString))
        }

        override def create(cohortItem: CohortItem): IO[Failure, Unit] = {
          dynamoDbZio
            .create(table = tableName, keyName = keyAttribName, value = cohortItem)
            .mapError {
              case DynamoDBZIOError(reason, _: Some[_]) =>
                CohortItemAlreadyPresentFailure(reason)
              case error => CohortCreateFailure(error.toString)
            }
        }

        override def update(cohortItem: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
          dynamoDbZio
            .update(table = tableName, key = CohortTableKey(cohortItem.subscriptionName), value = cohortItem)
            .mapError(error => CohortUpdateFailure(error.toString))
            .tapBoth(
              e => logging.error(s"Failed to update Cohort table: $e"),
              _ => logging.info(s"Wrote ${cohortItem} to Cohort table")
            )
        }

        override def fetchAll(): ZStream[Any, CohortFetchFailure, CohortItem] = {
          val queryRequest = ScanRequest.builder
            .tableName(tableName)
            .limit(cohortTableConfig.batchSize)
            .build()
          for {
            queryResults <- dynamoDbZio.scan(queryRequest).mapError(error => CohortFetchFailure(error.toString))
          } yield queryResults
        }.mapError(error => CohortFetchFailure(error.toString))
      }
    }
  }
}
