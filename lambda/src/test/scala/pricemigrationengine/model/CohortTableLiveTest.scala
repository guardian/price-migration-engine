package pricemigrationengine.model

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.services._
import software.amazon.awssdk.services.dynamodb.model.AttributeAction.PUT
import software.amazon.awssdk.services.dynamodb.model._
import zio.Exit.Success
import zio.stream.{Sink, ZStream}
import zio.{Chunk, IO, Runtime, ZIO, ZLayer}

import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.time.{Instant, LocalDate}
import scala.jdk.CollectionConverters._
import scala.util.Random

class CohortTableLiveTest extends munit.FunSuite {

  private val cohortSpec = CohortSpec(
    cohortName = "name",
    brazeCampaignName = "cmp123",
    importStartDate = LocalDate.of(2020, 1, 1),
    earliestPriceMigrationStartDate = LocalDate.of(2020, 1, 1)
  )

  val stubCohortTableConfiguration = ZLayer.succeed(
    new CohortTableConfiguration.Service {
      override val config: IO[ConfigurationFailure, CohortTableConfig] =
        IO.succeed(CohortTableConfig(10))
    }
  )

  val stubStageConfiguration = ZLayer.succeed(
    new StageConfiguration.Service {
      override val config: IO[ConfigurationFailure, StageConfig] =
        IO.succeed(StageConfig("DEV"))
    }
  )

  val expectedTableName = "PriceMigration-DEV-name"
  val expectedSubscriptionId = "subscription-id"
  val expectedProcessingStage = ReadyForEstimation
  val expectedStartDate = LocalDate.now.plusDays(Random.nextInt(365))
  val expectedCurrency = "GBP"
  val expectedOldPrice = Random.nextDouble()
  val expectedNewPrice = Random.nextDouble()
  val expectedEstimatedNewPrice = Random.nextDouble()
  val expectedBillingPeriod = "Monthly"
  val expectedWhenEstimationDone = Instant.ofEpochMilli(Random.nextLong())
  val expectedPriceRiseId = "price-rise-id"
  val expectedSfShowEstimate = Instant.ofEpochMilli(Random.nextLong())
  val expectedNewSubscriptionId = "new-sub-id"
  val expectedWhenAmendmentDone = Instant.ofEpochMilli(Random.nextLong())
  val expectedWhenNotificationSent = Instant.ofEpochMilli(Random.nextLong())
  val expectedWhenNotificationSentWrittenToSalesforce = Instant.ofEpochMilli(Random.nextLong())
  val item1 = CohortItem("subscription-1", ReadyForEstimation)
  val item2 = CohortItem("subscription-2", ReadyForEstimation)

  test("Query the PriceMigrationEngine with the correct filter and parse the results") {
    var receivedRequest: Option[QueryRequest] = None
    var receivedDeserialiser: Option[DynamoDBDeserialiser[CohortItem]] = None

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO.Service {
        override def query[A](
            query: QueryRequest
        )(implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A] = {
          receivedDeserialiser = Some(deserializer.asInstanceOf[DynamoDBDeserialiser[CohortItem]])
          receivedRequest = Some(query)
          ZStream(item1, item2).mapM(item => IO.effect(item.asInstanceOf[A]).orElseFail(DynamoDBZIOError("")))
        }

        override def update[A, B](table: String, key: A, value: B)(implicit
            keySerializer: DynamoDBSerialiser[A],
            valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = ???

        override def create[A](table: String, keyName: String, value: A)(implicit
            valueSerializer: DynamoDBSerialiser[A]
        ): IO[DynamoDBZIOError, Unit] = ???

        override def scan[A](query: ScanRequest)(implicit
            deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???
      }
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        for {
          result <-
            CohortTable
              .fetch(ReadyForEstimation, None)
              .provideLayer(
                stubCohortTableConfiguration ++ stubStageConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl >>>
                  CohortTableLive.impl(cohortSpec)
              )
          resultList <- result.run(Sink.collectAll[CohortItem])
          _ = assertEquals(resultList, Chunk(item1, item2))
        } yield ()
      ),
      Success(())
    )

    assertEquals(receivedRequest.get.tableName, expectedTableName)
    assertEquals(receivedRequest.get.indexName, "ProcessingStageIndexV2")
    assertEquals(receivedRequest.get.keyConditionExpression, "processingStage = :processingStage")
    assertEquals(
      receivedRequest.get.expressionAttributeValues,
      Map(":processingStage" -> AttributeValue.builder.s("ReadyForEstimation").build()).asJava
    )
    assertEquals(
      Runtime.default.unsafeRunSync(
        receivedDeserialiser.get.deserialise(
          Map(
            "subscriptionNumber" -> AttributeValue.builder.s(expectedSubscriptionId).build(),
            "processingStage" -> AttributeValue.builder.s(expectedProcessingStage.value).build(),
            "expectedStartDate" -> AttributeValue.builder.s(expectedStartDate.toString).build(),
            "currency" -> AttributeValue.builder.s(expectedCurrency).build(),
            "oldPrice" -> AttributeValue.builder.n(expectedOldPrice.toString).build(),
            "estimatedNewPrice" -> AttributeValue.builder.n(expectedEstimatedNewPrice.toString).build(),
            "billingPeriod" -> AttributeValue.builder.s(expectedBillingPeriod).build(),
            "whenEstimationDone" -> AttributeValue.builder.s(formatTimestamp(expectedWhenEstimationDone)).build(),
            "salesforcePriceRiseId" -> AttributeValue.builder.s(expectedPriceRiseId).build(),
            "whenSfShowEstimate" -> AttributeValue.builder.s(formatTimestamp(expectedSfShowEstimate)).build(),
            "startDate" -> AttributeValue.builder.s(expectedStartDate.toString).build(),
            "newPrice" -> AttributeValue.builder.n(expectedNewPrice.toString).build(),
            "newSubscriptionId" -> AttributeValue.builder.s(expectedNewSubscriptionId).build(),
            "whenAmendmentDone" -> AttributeValue.builder.s(formatTimestamp(expectedWhenAmendmentDone)).build(),
            "whenNotificationSent" -> AttributeValue.builder.s(formatTimestamp(expectedWhenNotificationSent)).build(),
            "whenNotificationSentWrittenToSalesforce" ->
              AttributeValue.builder.s(formatTimestamp(expectedWhenNotificationSentWrittenToSalesforce)).build()
          ).asJava
        )
      ),
      Success(
        CohortItem(
          subscriptionName = expectedSubscriptionId,
          processingStage = expectedProcessingStage,
          startDate = Some(expectedStartDate),
          currency = Some(expectedCurrency),
          oldPrice = Some(expectedOldPrice),
          estimatedNewPrice = Some(expectedEstimatedNewPrice),
          billingPeriod = Some(expectedBillingPeriod),
          whenEstimationDone = Some(expectedWhenEstimationDone),
          salesforcePriceRiseId = Some(expectedPriceRiseId),
          whenSfShowEstimate = Some(expectedSfShowEstimate),
          newPrice = Some(expectedNewPrice),
          newSubscriptionId = Some(expectedNewSubscriptionId),
          whenAmendmentDone = Some(expectedWhenAmendmentDone),
          whenNotificationSent = Some(expectedWhenNotificationSent),
          whenNotificationSentWrittenToSalesforce = Some(expectedWhenNotificationSentWrittenToSalesforce)
        )
      )
    )
  }

  test("Query the PriceMigrationEngine with the correct index for date range queries") {
    var receivedRequest: Option[QueryRequest] = None
    val expectedLatestDate = LocalDate.now()

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO.Service {
        override def query[A](
            query: QueryRequest
        )(implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A] = {
          receivedRequest = Some(query)
          ZStream(item1).mapM(item => IO.effect(item.asInstanceOf[A]).orElseFail(DynamoDBZIOError("")))
        }

        override def update[A, B](table: String, key: A, value: B)(implicit
            keySerializer: DynamoDBSerialiser[A],
            valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = ???

        override def create[A](table: String, keyName: String, value: A)(implicit
            valueSerializer: DynamoDBSerialiser[A]
        ): IO[DynamoDBZIOError, Unit] = ???

        override def scan[A](query: ScanRequest)(implicit
            deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???
      }
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        for {
          result <-
            CohortTable
              .fetch(ReadyForEstimation, Some(expectedLatestDate))
              .provideLayer(
                stubCohortTableConfiguration ++ stubStageConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl >>>
                  CohortTableLive.impl(cohortSpec)
              )
          resultList <- result.run(Sink.collectAll[CohortItem])
          _ = assertEquals(resultList, Chunk(item1))
        } yield ()
      ),
      Success(())
    )

    assertEquals(receivedRequest.get.tableName, expectedTableName)
    assertEquals(receivedRequest.get.indexName, "ProcessingStageStartDateIndexV1")
    assertEquals(
      receivedRequest.get.keyConditionExpression,
      "processingStage = :processingStage AND startDate <= :latestStartDateInclusive"
    )
    assertEquals(
      receivedRequest.get.expressionAttributeValues,
      Map(
        ":processingStage" -> AttributeValue.builder.s("ReadyForEstimation").build(),
        ":latestStartDateInclusive" -> AttributeValue.builder.s(expectedLatestDate.toString).build()
      ).asJava
    )
  }

  test("Update the PriceMigrationEngine table and serialise the CohortItem correctly") {
    var tableUpdated: Option[String] = None
    var receivedKey: Option[CohortTableKey] = None
    var receivedUpdate: Option[CohortItem] = None
    var receivedKeySerialiser: Option[DynamoDBSerialiser[CohortTableKey]] = None
    var receivedValueSerialiser: Option[DynamoDBUpdateSerialiser[CohortItem]] = None

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO.Service {
        override def query[A](query: QueryRequest)(implicit
            deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???

        override def update[A, B](table: String, key: A, value: B)(implicit
            keySerializer: DynamoDBSerialiser[A],
            valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = {
          tableUpdated = Some(table)
          receivedKey = Some(key.asInstanceOf[CohortTableKey])
          receivedUpdate = Some(value.asInstanceOf[CohortItem])
          receivedKeySerialiser = Some(keySerializer.asInstanceOf[DynamoDBSerialiser[CohortTableKey]])
          receivedValueSerialiser = Some(valueSerializer.asInstanceOf[DynamoDBUpdateSerialiser[CohortItem]])
          ZIO.effect(()).orElseFail(DynamoDBZIOError(""))
        }

        override def create[A](table: String, keyName: String, value: A)(implicit
            valueSerializer: DynamoDBSerialiser[A]
        ): IO[DynamoDBZIOError, Unit] = ???

        override def scan[A](query: ScanRequest)(implicit
            deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???
      }
    )

    val cohortItem = CohortItem(
      subscriptionName = expectedSubscriptionId,
      processingStage = expectedProcessingStage,
      currency = Some(expectedCurrency),
      oldPrice = Some(expectedOldPrice),
      newPrice = Some(expectedNewPrice),
      estimatedNewPrice = Some(expectedEstimatedNewPrice),
      billingPeriod = Some(expectedBillingPeriod),
      whenEstimationDone = Some(expectedWhenEstimationDone),
      salesforcePriceRiseId = Some(expectedPriceRiseId),
      whenSfShowEstimate = Some(expectedSfShowEstimate),
      startDate = Some(expectedStartDate),
      newSubscriptionId = Some(expectedNewSubscriptionId),
      whenAmendmentDone = Some(expectedWhenAmendmentDone),
      whenNotificationSent = Some(expectedWhenNotificationSent),
      whenNotificationSentWrittenToSalesforce = Some(expectedWhenNotificationSentWrittenToSalesforce)
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        CohortTable
          .update(cohortItem)
          .provideLayer(
            stubCohortTableConfiguration ++ stubStageConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl >>>
              CohortTableLive.impl(cohortSpec)
          )
      ),
      Success(())
    )

    assertEquals(tableUpdated.get, expectedTableName)
    assertEquals(receivedKey.get.subscriptionNumber, expectedSubscriptionId)
    assertEquals(
      receivedKeySerialiser.get.serialise(receivedKey.get),
      Map(
        "subscriptionNumber" -> AttributeValue.builder.s(expectedSubscriptionId).build()
      ).asJava
    )
    val update = receivedValueSerialiser.get.serialise(receivedUpdate.get)
    assertEquals(
      update.get("processingStage"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(expectedProcessingStage.value).build())
        .action(PUT)
        .build(),
      "processingStage"
    )
    assertEquals(
      update.get("currency"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(expectedCurrency).build())
        .action(PUT)
        .build(),
      "currency"
    )
    assertEquals(
      update.get("oldPrice"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.n(expectedOldPrice.toString).build())
        .action(PUT)
        .build(),
      "oldPrice"
    )
    assertEquals(
      update.get("newPrice"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.n(expectedNewPrice.toString).build())
        .action(PUT)
        .build(),
      "newPrice"
    )
    assertEquals(
      update.get("estimatedNewPrice"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.n(expectedEstimatedNewPrice.toString).build())
        .action(PUT)
        .build(),
      "estimatedNewPrice"
    )
    assertEquals(
      update.get("billingPeriod"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(expectedBillingPeriod).build())
        .action(PUT)
        .build(),
      "billingPeriod"
    )
    assertEquals(
      update.get("salesforcePriceRiseId"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(expectedPriceRiseId).build())
        .action(PUT)
        .build(),
      "salesforcePriceRiseId"
    )
    assertEquals(
      update.get("whenSfShowEstimate"),
      AttributeValueUpdate.builder
        .value(
          AttributeValue.builder
            .s(ISO_DATE_TIME.format(expectedSfShowEstimate.atZone(UTC)))
            .build()
        )
        .action(PUT)
        .build(),
      "whenSfShowEstimate"
    )
    assertEquals(
      update.get("startDate"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(expectedStartDate.toString).build())
        .action(PUT)
        .build(),
      "startDate"
    )
    assertEquals(
      update.get("newSubscriptionId"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(expectedNewSubscriptionId).build())
        .action(PUT)
        .build(),
      "newSubscriptionId"
    )
    assertEquals(
      update.get("whenAmendmentDone"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(formatTimestamp(expectedWhenAmendmentDone)).build())
        .action(PUT)
        .build(),
      "whenAmendmentDone"
    )
    assertEquals(
      update.get("whenNotificationSentWrittenToSalesforce"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(formatTimestamp(expectedWhenNotificationSentWrittenToSalesforce)).build())
        .action(PUT)
        .build(),
      "whenNotificationSentWrittenToSalesforce"
    )
    assertEquals(
      update.get("whenNotificationSent"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(formatTimestamp(expectedWhenNotificationSent)).build())
        .action(PUT)
        .build(),
      "whenNotificationSent"
    )
  }

  private def formatTimestamp(instant: Instant) = {
    ISO_DATE_TIME.format(instant.atZone(UTC))
  }

  test("Update the PriceMigrationEngine table and serialise the CohortItem with missing optional values correctly") {
    var receivedUpdate: Option[CohortItem] = None
    var receivedValueSerialiser: Option[DynamoDBUpdateSerialiser[CohortItem]] = None

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO.Service {
        override def query[A](query: QueryRequest)(implicit
            deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???

        override def update[A, B](table: String, key: A, value: B)(implicit
            keySerializer: DynamoDBSerialiser[A],
            valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = {
          receivedValueSerialiser = Some(valueSerializer.asInstanceOf[DynamoDBUpdateSerialiser[CohortItem]])
          receivedUpdate = Some(value.asInstanceOf[CohortItem])
          ZIO.effect(()).orElseFail(DynamoDBZIOError(""))
        }

        override def create[A](table: String, keyName: String, value: A)(implicit
            valueSerializer: DynamoDBSerialiser[A]
        ): IO[DynamoDBZIOError, Unit] = ???

        override def scan[A](query: ScanRequest)(implicit
            deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???
      }
    )

    val expectedSubscriptionId = "subscription-id"
    val expectedProcessingStage = ReadyForEstimation

    val cohortItem = CohortItem(
      subscriptionName = expectedSubscriptionId,
      processingStage = expectedProcessingStage
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        CohortTable
          .update(cohortItem)
          .provideLayer(
            stubStageConfiguration ++ stubCohortTableConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl >>>
              CohortTableLive.impl(cohortSpec)
          )
      ),
      Success(())
    )

    val update = receivedValueSerialiser.get.serialise(receivedUpdate.get).asScala
    assertEquals(
      update.get("processingStage"),
      Some(
        AttributeValueUpdate.builder
          .value(AttributeValue.builder.s(expectedProcessingStage.value).build())
          .action(PUT)
          .build()
      ),
      "processingStage"
    )
    assertEquals(update.get("currency"), None, "currency")
    assertEquals(update.get("oldPrice"), None, "oldPrice")
    assertEquals(update.get("newPrice"), None, "newPrice")
    assertEquals(update.get("estimatedNewPrice"), None, "estimatedNewPrice")
    assertEquals(update.get("billingPeriod"), None, "billingPeriod")
    assertEquals(update.get("salesforcePriceRiseId"), None, "salesforcePriceRiseId")
    assertEquals(update.get("whenSfShowEstimate"), None, "whenSfShowEstimate")
    assertEquals(update.get("startDate"), None, "startDate")
    assertEquals(update.get("newSubscriptionId"), None, "newSubscriptionId")
    assertEquals(update.get("whenAmendmentDone"), None, "whenAmendmentDone")
  }

  test("Create the cohort item correctly") {
    var tableUpdated: Option[String] = None
    var receivedInsert: Option[CohortItem] = None
    var receivedSerialiser: Option[DynamoDBSerialiser[CohortItem]] = None

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO.Service {
        override def query[A](query: QueryRequest)(implicit
            deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???

        override def update[A, B](table: String, key: A, value: B)(implicit
            keySerializer: DynamoDBSerialiser[A],
            valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = ???

        override def create[A](table: String, keyName: String, value: A)(implicit
            valueSerializer: DynamoDBSerialiser[A]
        ): IO[DynamoDBZIOError, Unit] = {
          tableUpdated = Some(table)
          receivedInsert = Some(value.asInstanceOf[CohortItem])
          receivedSerialiser = Some(valueSerializer.asInstanceOf[DynamoDBSerialiser[CohortItem]])
          ZIO.effect(()).orElseFail(DynamoDBZIOError(""))
        }

        override def scan[A](query: ScanRequest)(implicit
            deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???
      }
    )

    val cohortItem = CohortItem("Subscription-id", ReadyForEstimation)

    assertEquals(
      Runtime.default.unsafeRunSync(
        CohortTable
          .create(cohortItem)
          .provideLayer(
            stubStageConfiguration ++ stubCohortTableConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl >>>
              CohortTableLive.impl(cohortSpec)
          )
      ),
      Success(())
    )

    assertEquals(tableUpdated.get, expectedTableName)
    val insert = receivedSerialiser.get.serialise(receivedInsert.get)
    assertEquals(insert.get("subscriptionNumber"), AttributeValue.builder.s("Subscription-id").build())
    assertEquals(insert.get("processingStage"), AttributeValue.builder.s("ReadyForEstimation").build())
  }
}
