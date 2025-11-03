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
  private val ProcessingStageAndDateIndexName = "ProcessingStageAndDateIndexV1"

  private implicit val cohortItemDeserialiser: DynamoDBDeserialiser[CohortItem] = { cohortItem =>
    ZIO
      .fromEither(
        for {
          subscriptionNumber <- getStringFromResults(cohortItem, keyAttribName)
          processingStage <- getCohortTableFilter(cohortItem, "processingStage")
          amendmentEffectiveDate <- getOptionalDateFromResults(cohortItem, "amendmentEffectiveDate")
          currency <- getOptionalStringFromResults(cohortItem, "currency")
          oldPrice <- getOptionalBigDecimalFromResults(cohortItem, "oldPrice")
          estimatedNewPrice <- getOptionalBigDecimalFromResults(cohortItem, "estimatedNewPrice")
          billingPeriod <- getOptionalStringFromResults(cohortItem, "billingPeriod")
          whenEstimationDone <- getOptionalInstantFromResults(cohortItem, "whenEstimationDone")
          salesforcePriceRiseId <- getOptionalStringFromResults(cohortItem, "salesforcePriceRiseId")
          whenSfShowEstimate <- getOptionalInstantFromResults(cohortItem, "whenSfShowEstimate")
          commsPrice <- getOptionalBigDecimalFromResults(cohortItem, "commsPrice")
          newPrice <- getOptionalBigDecimalFromResults(cohortItem, "newPrice")
          newSubscriptionId <- getOptionalStringFromResults(cohortItem, "newSubscriptionId")
          whenAmendmentDone <- getOptionalInstantFromResults(cohortItem, "whenAmendmentDone")
          whenNotificationSent <- getOptionalInstantFromResults(cohortItem, "whenNotificationSent")
          whenNotificationSentWrittenToSalesforce <-
            getOptionalInstantFromResults(cohortItem, "whenNotificationSentWrittenToSalesforce")
          cancellationReason <-
            getOptionalStringFromResults(cohortItem, "cancellationReason")
          migrationExtraAttributes <- getOptionalStringFromResults(cohortItem, "migrationExtraAttributes")
          ex_2025N4_label <- getOptionalStringFromResults(cohortItem, "ex_2025N4_label")
          ex_2025N4_group <- getOptionalStringFromResults(cohortItem, "ex_2025N4_group")
          ex_2025N4_canvas <- getOptionalStringFromResults(cohortItem, "ex_2025N4_canvas")
          ex_2025N4_rateplan_current <- getOptionalStringFromResults(cohortItem, "ex_2025N4_rateplan_current")
          ex_2025N4_rateplan_target <- getOptionalStringFromResults(cohortItem, "ex_2025N4_rateplan_target")
          ex_membership2025_country <- getOptionalStringFromResults(cohortItem, "ex_membership2025_country")
        } yield CohortItem(
          subscriptionName = subscriptionNumber,
          processingStage = processingStage,
          amendmentEffectiveDate = amendmentEffectiveDate,
          currency = currency,
          oldPrice = oldPrice,
          commsPrice = commsPrice,
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
          migrationExtraAttributes = migrationExtraAttributes,
          ex_2025N4_label = ex_2025N4_label,
          ex_2025N4_group = ex_2025N4_group,
          ex_2025N4_canvas = ex_2025N4_canvas,
          ex_2025N4_rateplan_current = ex_2025N4_rateplan_current,
          ex_2025N4_rateplan_target = ex_2025N4_rateplan_target,
          ex_membership2025_country = ex_membership2025_country
        )
      )
      .mapError(e => DynamoDBZIOError(e))
  }

  private implicit val cohortItemUpdateSerialiser: DynamoDBUpdateSerialiser[CohortItem] =
    cohortItem =>
      List(
        Option(stringFieldUpdate("processingStage", cohortItem.processingStage.value)),
        cohortItem.amendmentEffectiveDate.map(amendmentEffectiveDate =>
          dateFieldUpdate("amendmentEffectiveDate", amendmentEffectiveDate)
        ),
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
        cohortItem.newPrice.map(newPrice => bigDecimalFieldUpdate("newPrice", newPrice)),
        cohortItem.commsPrice.map(commsPrice => bigDecimalFieldUpdate("commsPrice", commsPrice)),
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
        cohortItem.migrationExtraAttributes.map(extra => stringFieldUpdate("migrationExtraAttributes", extra)),
        cohortItem.ex_2025N4_label.map(value => stringFieldUpdate("ex_2025N4_label", value)),
        cohortItem.ex_2025N4_group.map(value => stringFieldUpdate("ex_2025N4_group", value)),
        cohortItem.ex_2025N4_canvas.map(value => stringFieldUpdate("ex_2025N4_canvas", value)),
        cohortItem.ex_2025N4_rateplan_current.map(value => stringFieldUpdate("ex_2025N4_rateplan_current", value)),
        cohortItem.ex_2025N4_rateplan_target.map(value => stringFieldUpdate("ex_2025N4_rateplan_target", value)),
        cohortItem.ex_membership2025_country.map(value => stringFieldUpdate("ex_membership2025_country", value))
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
            latestAmendmentEffectiveDateInclusive: Option[LocalDate]
        ): ZStream[Any, CohortFetchFailure, CohortItem] = {
          val indexName =
            latestAmendmentEffectiveDateInclusive
              .fold(ProcessingStageIndexName)(_ => ProcessingStageAndDateIndexName)
          val queryRequest =
            QueryRequest.builder
              .tableName(tableName)
              .indexName(indexName)
              .keyConditionExpression(
                "processingStage = :processingStage" + latestAmendmentEffectiveDateInclusive.fold("") { _ =>
                  " AND amendmentEffectiveDate <= :date"
                }
              )
              .expressionAttributeValues(
                List(
                  Some(":processingStage" -> AttributeValue.builder.s(filter.value).build()),
                  latestAmendmentEffectiveDateInclusive.map { date =>
                    ":date" -> AttributeValue.builder
                      .s(date.toString)
                      .build()
                  }
                ).flatten.toMap.asJava
              )
              .limit(cohortTableConfig.batchSize)
              .build()
          logging.info(s"[72E0D1FC] queryRequest: ${queryRequest.toString}")
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
