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
    IO.fromEither(
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
        whenNotificationSentWrittenToSalesforce = whenNotificationSentWrittenToSalesforce
      )
    ).mapError(e => DynamoDBZIOError(e))
  }

  private implicit val cohortItemUpdateSerialiser: DynamoDBUpdateSerialiser[CohortItem] =
    cohortItem =>
      List(
        Option(stringFieldUpdate("processingStage", cohortItem.processingStage.value)),
        cohortItem.startDate
          .map(startDate => dateFieldUpdate("startDate", startDate)),
        cohortItem.currency
          .map(currency => stringFieldUpdate("currency", currency)),
        cohortItem.oldPrice
          .map(oldPrice => bigDecimalFieldUpdate("oldPrice", oldPrice)),
        cohortItem.estimatedNewPrice
          .map(estimatedNewPrice => bigDecimalFieldUpdate("estimatedNewPrice", estimatedNewPrice)),
        cohortItem.billingPeriod.map(billingPeriod => stringFieldUpdate("billingPeriod", billingPeriod)),
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
        )
      ).flatten.toMap.asJava

  private implicit val cohortTableKeySerialiser: DynamoDBSerialiser[CohortTableKey] =
    key => Map(stringUpdate(keyAttribName, key.subscriptionNumber)).asJava

  private implicit val cohortTableSerialiser: DynamoDBSerialiser[CohortItem] =
    cohortItem =>
      Map(
        stringUpdate(keyAttribName, cohortItem.subscriptionName),
        stringUpdate("processingStage", cohortItem.processingStage.value)
      ).asJava

  def impl(cohortSpec: CohortSpec): ZLayer[
    DynamoDBZIO with StageConfiguration with CohortTableConfiguration with Logging,
    ConfigurationFailure,
    CohortTable
  ] = {
    ZLayer.fromZIO {
      for {
        dynamoDbZio <- ZIO.service[DynamoDBZIO]
        config <- StageConfiguration.stageConfig
        tableName = cohortSpec.tableName(config.stage)
        cohortTableConfig <- CohortTableConfiguration.cohortTableConfig
        logging <- ZIO.service[Logging]
      } yield new CohortTable.Service {
        override def fetch(
            filter: CohortTableFilter,
            latestStartDateInclusive: Option[LocalDate]
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          ZIO
            .from {
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
            .mapError(error => CohortFetchFailure(error.toString))
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

        override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
          dynamoDbZio
            .update(table = tableName, key = CohortTableKey(result.subscriptionName), value = result)
            .mapError(error => CohortUpdateFailure(error.toString))
            .tapBoth(
              e => logging.error(s"Failed to update Cohort table: $e"),
              _ => logging.info(s"Wrote $result to Cohort table")
            )
        }

        override def fetchAll(): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          ZIO
            .from {
              val queryRequest = ScanRequest.builder
                .tableName(s"PriceMigrationEngine${config.stage}")
                .limit(cohortTableConfig.batchSize)
                .build()
              for {
                queryResults <- dynamoDbZio.scan(queryRequest).mapError(error => CohortFetchFailure(error.toString))
              } yield queryResults
            }
            .mapError(error => CohortFetchFailure(error.toString))
        }
      }
    }
  }
}
