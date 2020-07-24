package pricemigrationengine.services

import java.time.LocalDate

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest, ScanRequest}
import pricemigrationengine.model._
import pricemigrationengine.model.dynamodb.Conversions._
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

object CohortTableLive {
  private val ProcessingStageIndexName = "ProcessingStageIndexV2"

  private val ProcessingStageAndStartDateIndexName = "ProcessingStageStartDateIndexV1"

  private implicit val cohortItemDeserialiser: DynamoDBDeserialiser[CohortItem] = { cohortItem =>
    import scala.language.implicitConversions
    implicit def errorMapped[A](orig: Either[String, A]): IO[DynamoDBZIOError, A] =
      ZIO.fromEither(orig).mapError(DynamoDBZIOError)
    for {
      subscriptionNumber <- getStringFromResults(cohortItem, "subscriptionNumber")
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
    key => Map(stringUpdate("subscriptionNumber", key.subscriptionNumber)).asJava

  private implicit val cohortTableSerialiser: DynamoDBSerialiser[CohortItem] =
    cohortItem =>
      Map(
        stringUpdate("subscriptionNumber", cohortItem.subscriptionName),
        stringUpdate("processingStage", cohortItem.processingStage.value)
      ).asJava

  def impl(cohortSpec: CohortSpec): ZLayer[
    DynamoDBZIO with StageConfiguration with CohortTableConfiguration with Logging,
    ConfigurationFailure,
    CohortTable
  ] = {
    ZLayer.fromFunctionM {
      dependencies: DynamoDBZIO with StageConfiguration with CohortTableConfiguration with Logging => {
        for {
         config <- StageConfiguration.stageConfig
         tableName = cohortSpec.tableName(config.stage)
         cohortTableConfig <- CohortTableConfiguration.cohortTableConfig
        } yield new CohortTable.Service {
            override def fetch(
                filter: CohortTableFilter,
                latestStartDateInclusive: Option[LocalDate]
            ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
              val indexName =
                latestStartDateInclusive
                  .fold(ProcessingStageIndexName)(_ => ProcessingStageAndStartDateIndexName)
              val queryRequest =
                new QueryRequest()
                  .withTableName(tableName)
                  .withIndexName(indexName)
                  .withKeyConditionExpression(
                    "processingStage = :processingStage" + latestStartDateInclusive.fold("") { _ =>
                      " AND startDate <= :latestStartDateInclusive"
                    }
                  )
                  .withExpressionAttributeValues(
                    List(
                      Some(":processingStage" -> new AttributeValue(filter.value)),
                      latestStartDateInclusive.map { latestStartDateInclusive =>
                        ":latestStartDateInclusive" -> new AttributeValue(latestStartDateInclusive.toString)
                      }
                    ).flatten.toMap.asJava
                  )
                  .withLimit(cohortTableConfig.batchSize)
              DynamoDBZIO.query(queryRequest).map(_.mapError(error => CohortFetchFailure(error.toString)))
            }.provide(dependencies)

            override def put(cohortItem: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
              for {
                result <-
                  DynamoDBZIO
                    .put(table = tableName, value = cohortItem)
                    .mapError(error => CohortUpdateFailure(error.toString))
              } yield result
            }.provide(dependencies)

            override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
              (for {
                result <-
                  DynamoDBZIO
                    .update(table = tableName, key = CohortTableKey(result.subscriptionName), value = result)
                    .mapError(error => CohortUpdateFailure(error.toString))
              } yield result)
                .tapBoth(
                  e => Logging.error(s"Failed to update Cohort table: $e"),
                  _ => Logging.info(s"Wrote $result to Cohort table")
                )
            }.provide(dependencies)

            override def fetchAll(): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
              for {
                cohortTableConfig <- CohortTableConfiguration.cohortTableConfig
                  .mapError(error => CohortFetchFailure(s"Failed to get configuration:${error.reason}"))
                stageConfig <- StageConfiguration.stageConfig
                  .mapError(error => CohortFetchFailure(s"Failed to get configuration:${error.reason}"))
                queryRequest = new ScanRequest()
                  .withTableName(s"PriceMigrationEngine${stageConfig.stage}")
                  .withLimit(cohortTableConfig.batchSize)
                queryResults <- DynamoDBZIO
                  .scan(
                    queryRequest
                  )
                  .map(_.mapError(error => CohortFetchFailure(error.toString)))
              } yield queryResults
            }.provide(dependencies)
          }
      }.provide(dependencies)
    }
  }
}
