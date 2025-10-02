package pricemigrationengine.model

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.services._
import software.amazon.awssdk.services.dynamodb.model.AttributeAction.PUT
import software.amazon.awssdk.services.dynamodb.model._
import zio.Exit.Success
import zio.stream.{ZSink, ZStream}
import zio.{Chunk, IO, Runtime, ZIO, ZLayer}

import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.time.{Instant, LocalDate}
import scala.jdk.CollectionConverters._
import scala.util.Random

class CohortTableLiveTest extends munit.FunSuite {

  private val cohortSpec = CohortSpec(
    cohortName = "name",
    brazeName = "cmp123",
    earliestAmendmentEffectiveDate = LocalDate.of(2020, 1, 1)
  )

  val stubCohortTableConfiguration = ZLayer.succeed(CohortTableConfig(10))
  val stubStageConfiguration = ZLayer.succeed(StageConfig("DEV"))

  val tableName = "PriceMigration-DEV-name"
  val subscriptionId = "subscription-id"
  val processingStage = ReadyForEstimation
  val amendmentEffectiveDate = LocalDate.now.plusDays(Random.nextInt(365))
  val currency = "GBP"
  val oldPrice = Random.nextDouble()
  val newPrice = Random.nextDouble()
  val estimatedNewPrice = Random.nextDouble()
  val billingPeriod = "Monthly"
  val whenEstimationDone = Instant.ofEpochMilli(Random.nextLong())
  val priceRiseId = "price-rise-id"
  val sfShowEstimate = Instant.ofEpochMilli(Random.nextLong())
  val newSubscriptionId = "new-sub-id"
  val whenAmendmentDone = Instant.ofEpochMilli(Random.nextLong())
  val whenNotificationSent = Instant.ofEpochMilli(Random.nextLong())
  val whenNotificationSentWrittenToSalesforce = Instant.ofEpochMilli(Random.nextLong())
  val item1 = CohortItem("subscription-1", ReadyForEstimation)
  val item2 = CohortItem("subscription-2", ReadyForEstimation)

  test("Query the PriceMigrationEngine with the correct filter and parse the results") {
    var receivedRequest: Option[QueryRequest] = None
    var receivedDeserialiser: Option[DynamoDBDeserialiser[CohortItem]] = None

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO {

        override def query[A](
            query: QueryRequest
        )(implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A] = {
          receivedDeserialiser = Some(deserializer.asInstanceOf[DynamoDBDeserialiser[CohortItem]])
          receivedRequest = Some(query)
          ZStream(item1, item2).mapZIO(item => ZIO.attempt(item.asInstanceOf[A]).orElseFail(DynamoDBZIOError("")))
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
      Runner.unsafeRunSync(Runtime.default)(
        for {
          resultList <- CohortTable
            .fetch(ReadyForEstimation, None)
            .provideLayer(
              stubCohortTableConfiguration ++ stubStageConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl(
                "TestCohort"
              ) >>> CohortTableLive.impl(cohortSpec)
            )
            .run(ZSink.collectAll[CohortItem])
          _ = assertEquals(resultList, Chunk(item1, item2))
        } yield ()
      ),
      Success(())
    )

    assertEquals(receivedRequest.get.tableName, tableName)
    assertEquals(receivedRequest.get.indexName, "ProcessingStageIndexV2")
    assertEquals(receivedRequest.get.keyConditionExpression, "processingStage = :processingStage")
    assertEquals(
      receivedRequest.get.expressionAttributeValues,
      Map(":processingStage" -> AttributeValue.builder.s("ReadyForEstimation").build()).asJava
    )
    assertEquals(
      Runner.unsafeRunSync(Runtime.default)(
        receivedDeserialiser.get.deserialise(
          Map(
            "subscriptionNumber" -> AttributeValue.builder.s(subscriptionId).build(),
            "processingStage" -> AttributeValue.builder.s(processingStage.value).build(),
            "currency" -> AttributeValue.builder.s(currency).build(),
            "oldPrice" -> AttributeValue.builder.n(oldPrice.toString).build(),
            "estimatedNewPrice" -> AttributeValue.builder.n(estimatedNewPrice.toString).build(),
            "billingPeriod" -> AttributeValue.builder.s(billingPeriod).build(),
            "whenEstimationDone" -> AttributeValue.builder.s(formatTimestamp(whenEstimationDone)).build(),
            "salesforcePriceRiseId" -> AttributeValue.builder.s(priceRiseId).build(),
            "whenSfShowEstimate" -> AttributeValue.builder.s(formatTimestamp(sfShowEstimate)).build(),
            "amendmentEffectiveDate" -> AttributeValue.builder.s(amendmentEffectiveDate.toString).build(),
            "newPrice" -> AttributeValue.builder.n(newPrice.toString).build(),
            "newSubscriptionId" -> AttributeValue.builder.s(newSubscriptionId).build(),
            "whenAmendmentDone" -> AttributeValue.builder.s(formatTimestamp(whenAmendmentDone)).build(),
            "whenNotificationSent" -> AttributeValue.builder.s(formatTimestamp(whenNotificationSent)).build(),
            "whenNotificationSentWrittenToSalesforce" ->
              AttributeValue.builder.s(formatTimestamp(whenNotificationSentWrittenToSalesforce)).build()
          ).asJava
        )
      ),
      Success(
        CohortItem(
          subscriptionName = subscriptionId,
          processingStage = processingStage,
          amendmentEffectiveDate = Some(amendmentEffectiveDate),
          currency = Some(currency),
          oldPrice = Some(oldPrice),
          estimatedNewPrice = Some(estimatedNewPrice),
          billingPeriod = Some(billingPeriod),
          whenEstimationDone = Some(whenEstimationDone),
          salesforcePriceRiseId = Some(priceRiseId),
          whenSfShowEstimate = Some(sfShowEstimate),
          newPrice = Some(newPrice),
          newSubscriptionId = Some(newSubscriptionId),
          whenAmendmentDone = Some(whenAmendmentDone),
          whenNotificationSent = Some(whenNotificationSent),
          whenNotificationSentWrittenToSalesforce = Some(whenNotificationSentWrittenToSalesforce)
        )
      )
    )
  }

  test("Query the PriceMigrationEngine with the correct index for date range queries") {
    var receivedRequest: Option[QueryRequest] = None
    val expectedLatestDate = LocalDate.now()

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO {

        override def query[A](
            query: QueryRequest
        )(implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A] = {
          receivedRequest = Some(query)
          ZStream(item1).mapZIO(item => ZIO.attempt(item.asInstanceOf[A]).orElseFail(DynamoDBZIOError("")))
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
      Runner.unsafeRunSync(Runtime.default)(
        for {
          resultList <- CohortTable
            .fetch(ReadyForEstimation, Some(expectedLatestDate))
            .provideLayer(
              stubCohortTableConfiguration ++ stubStageConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl(
                "TestCohort"
              ) >>> CohortTableLive.impl(cohortSpec)
            )
            .run(ZSink.collectAll[CohortItem])
          _ = assertEquals(resultList, Chunk(item1))
        } yield ()
      ),
      Success(())
    )

    assertEquals(receivedRequest.get.tableName, tableName)
    assertEquals(receivedRequest.get.indexName, "ProcessingStageStartDateIndexV1")
    assertEquals(
      receivedRequest.get.keyConditionExpression,
      "processingStage = :processingStage AND amendmentEffectiveDate <= :date"
    )
    assertEquals(
      receivedRequest.get.expressionAttributeValues,
      Map(
        ":processingStage" -> AttributeValue.builder.s("ReadyForEstimation").build(),
        ":date" -> AttributeValue.builder.s(expectedLatestDate.toString).build()
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
      new DynamoDBZIO {

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
          ZIO.attempt(()).orElseFail(DynamoDBZIOError(""))
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
      subscriptionName = subscriptionId,
      processingStage = processingStage,
      currency = Some(currency),
      oldPrice = Some(oldPrice),
      newPrice = Some(newPrice),
      estimatedNewPrice = Some(estimatedNewPrice),
      billingPeriod = Some(billingPeriod),
      whenEstimationDone = Some(whenEstimationDone),
      salesforcePriceRiseId = Some(priceRiseId),
      whenSfShowEstimate = Some(sfShowEstimate),
      amendmentEffectiveDate = Some(amendmentEffectiveDate),
      newSubscriptionId = Some(newSubscriptionId),
      whenAmendmentDone = Some(whenAmendmentDone),
      whenNotificationSent = Some(whenNotificationSent),
      whenNotificationSentWrittenToSalesforce = Some(whenNotificationSentWrittenToSalesforce)
    )

    assertEquals(
      Runner.unsafeRunSync(Runtime.default)(
        CohortTable
          .update(cohortItem)
          .provideLayer(
            stubCohortTableConfiguration ++ stubStageConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging
              .impl("TestCohort") >>>
              CohortTableLive.impl(cohortSpec)
          )
      ),
      Success(())
    )

    assertEquals(tableUpdated.get, tableName)
    assertEquals(receivedKey.get.subscriptionNumber, subscriptionId)
    assertEquals(
      receivedKeySerialiser.get.serialise(receivedKey.get),
      Map(
        "subscriptionNumber" -> AttributeValue.builder.s(subscriptionId).build()
      ).asJava
    )

    val update = receivedValueSerialiser.get.serialise(receivedUpdate.get)
    assertEquals(
      update.get("processingStage"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(processingStage.value).build())
        .action(PUT)
        .build(),
      "processingStage"
    )
    assertEquals(
      update.get("currency"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(currency).build())
        .action(PUT)
        .build(),
      "currency"
    )
    assertEquals(
      update.get("oldPrice"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.n(oldPrice.toString).build())
        .action(PUT)
        .build(),
      "oldPrice"
    )
    assertEquals(
      update.get("newPrice"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.n(newPrice.toString).build())
        .action(PUT)
        .build(),
      "newPrice"
    )
    assertEquals(
      update.get("estimatedNewPrice"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.n(estimatedNewPrice.toString).build())
        .action(PUT)
        .build(),
      "estimatedNewPrice"
    )
    assertEquals(
      update.get("billingPeriod"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(billingPeriod).build())
        .action(PUT)
        .build(),
      "billingPeriod"
    )
    assertEquals(
      update.get("salesforcePriceRiseId"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(priceRiseId).build())
        .action(PUT)
        .build(),
      "salesforcePriceRiseId"
    )
    assertEquals(
      update.get("whenSfShowEstimate"),
      AttributeValueUpdate.builder
        .value(
          AttributeValue.builder
            .s(ISO_DATE_TIME.format(sfShowEstimate.atZone(UTC)))
            .build()
        )
        .action(PUT)
        .build(),
      "whenSfShowEstimate"
    )
    assertEquals(
      update.get("amendmentEffectiveDate"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(amendmentEffectiveDate.toString).build())
        .action(PUT)
        .build(),
      "amendmentEffectiveDate"
    )
    assertEquals(
      update.get("newSubscriptionId"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(newSubscriptionId).build())
        .action(PUT)
        .build(),
      "newSubscriptionId"
    )
    assertEquals(
      update.get("whenAmendmentDone"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(formatTimestamp(whenAmendmentDone)).build())
        .action(PUT)
        .build(),
      "whenAmendmentDone"
    )
    assertEquals(
      update.get("whenNotificationSentWrittenToSalesforce"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(formatTimestamp(whenNotificationSentWrittenToSalesforce)).build())
        .action(PUT)
        .build(),
      "whenNotificationSentWrittenToSalesforce"
    )
    assertEquals(
      update.get("whenNotificationSent"),
      AttributeValueUpdate.builder
        .value(AttributeValue.builder.s(formatTimestamp(whenNotificationSent)).build())
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
      new DynamoDBZIO {

        override def query[A](query: QueryRequest)(implicit
            deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???

        override def update[A, B](table: String, key: A, value: B)(implicit
            keySerializer: DynamoDBSerialiser[A],
            valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = {
          receivedValueSerialiser = Some(valueSerializer.asInstanceOf[DynamoDBUpdateSerialiser[CohortItem]])
          receivedUpdate = Some(value.asInstanceOf[CohortItem])
          ZIO.attempt(()).orElseFail(DynamoDBZIOError(""))
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
      Runner.unsafeRunSync(Runtime.default)(
        CohortTable
          .update(cohortItem)
          .provideLayer(
            stubStageConfiguration ++ stubCohortTableConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging
              .impl("TestCohort") >>>
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
      new DynamoDBZIO {

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
          ZIO.attempt(()).orElseFail(DynamoDBZIOError(""))
        }

        override def scan[A](query: ScanRequest)(implicit
            deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???
      }
    )

    val cohortItem = CohortItem("Subscription-id", ReadyForEstimation)

    assertEquals(
      Runner.unsafeRunSync(Runtime.default)(
        CohortTable
          .create(cohortItem)
          .provideLayer(
            stubStageConfiguration ++ stubCohortTableConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging
              .impl("TestCohort") >>>
              CohortTableLive.impl(cohortSpec)
          )
      ),
      Success(())
    )

    assertEquals(tableUpdated.get, tableName)
    val insert = receivedSerialiser.get.serialise(receivedInsert.get)
    assertEquals(insert.get("subscriptionNumber"), AttributeValue.builder.s("Subscription-id").build())
    assertEquals(insert.get("processingStage"), AttributeValue.builder.s("ReadyForEstimation").build())
  }
}
