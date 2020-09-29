package pricemigrationengine.model

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset}

import com.amazonaws.services.dynamodbv2.model.{
  AttributeAction,
  AttributeValue,
  AttributeValueUpdate,
  QueryRequest,
  ScanRequest
}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.services._
import zio.Exit.Success
import zio.stream.{Sink, ZStream}
import zio.{Chunk, IO, Runtime, ZIO, ZLayer}

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

    assertEquals(receivedRequest.get.getTableName, expectedTableName)
    assertEquals(receivedRequest.get.getIndexName, "ProcessingStageIndexV2")
    assertEquals(receivedRequest.get.getKeyConditionExpression, "processingStage = :processingStage")
    assertEquals(
      receivedRequest.get.getExpressionAttributeValues,
      Map(":processingStage" -> new AttributeValue().withS("ReadyForEstimation")).asJava
    )
    assertEquals(
      Runtime.default.unsafeRunSync(
        receivedDeserialiser.get.deserialise(
          Map(
            "subscriptionNumber" -> new AttributeValue().withS(expectedSubscriptionId),
            "processingStage" -> new AttributeValue().withS(expectedProcessingStage.value),
            "expectedStartDate" -> new AttributeValue().withS(expectedStartDate.toString),
            "currency" -> new AttributeValue().withS(expectedCurrency),
            "oldPrice" -> new AttributeValue().withN(expectedOldPrice.toString),
            "estimatedNewPrice" -> new AttributeValue().withN(expectedEstimatedNewPrice.toString),
            "billingPeriod" -> new AttributeValue().withS(expectedBillingPeriod),
            "whenEstimationDone" -> new AttributeValue().withS(formatTimestamp(expectedWhenEstimationDone)),
            "salesforcePriceRiseId" -> new AttributeValue().withS(expectedPriceRiseId),
            "whenSfShowEstimate" -> new AttributeValue().withS(formatTimestamp(expectedSfShowEstimate)),
            "startDate" -> new AttributeValue().withS(expectedStartDate.toString),
            "newPrice" -> new AttributeValue().withN(expectedNewPrice.toString),
            "newSubscriptionId" -> new AttributeValue().withS(expectedNewSubscriptionId),
            "whenAmendmentDone" -> new AttributeValue().withS(formatTimestamp(expectedWhenAmendmentDone)),
            "whenNotificationSent" -> new AttributeValue().withS(formatTimestamp(expectedWhenNotificationSent)),
            "whenNotificationSentWrittenToSalesforce" ->
              new AttributeValue().withS(formatTimestamp(expectedWhenNotificationSentWrittenToSalesforce))
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

    assertEquals(receivedRequest.get.getTableName, expectedTableName)
    assertEquals(receivedRequest.get.getIndexName, "ProcessingStageStartDateIndexV1")
    assertEquals(
      receivedRequest.get.getKeyConditionExpression,
      "processingStage = :processingStage AND startDate <= :latestStartDateInclusive"
    )
    assertEquals(
      receivedRequest.get.getExpressionAttributeValues,
      Map(
        ":processingStage" -> new AttributeValue().withS("ReadyForEstimation"),
        ":latestStartDateInclusive" -> new AttributeValue().withS(expectedLatestDate.toString)
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
        "subscriptionNumber" -> new AttributeValue().withS(expectedSubscriptionId)
      ).asJava
    )
    val update = receivedValueSerialiser.get.serialise(receivedUpdate.get)
    assertEquals(
      update.get("processingStage"),
      new AttributeValueUpdate(new AttributeValue().withS(expectedProcessingStage.value), AttributeAction.PUT),
      "processingStage"
    )
    assertEquals(
      update.get("currency"),
      new AttributeValueUpdate(new AttributeValue().withS(expectedCurrency), AttributeAction.PUT),
      "currency"
    )
    assertEquals(
      update.get("oldPrice"),
      new AttributeValueUpdate(new AttributeValue().withN(expectedOldPrice.toString), AttributeAction.PUT),
      "oldPrice"
    )
    assertEquals(
      update.get("newPrice"),
      new AttributeValueUpdate(new AttributeValue().withN(expectedNewPrice.toString), AttributeAction.PUT),
      "newPrice"
    )
    assertEquals(
      update.get("estimatedNewPrice"),
      new AttributeValueUpdate(new AttributeValue().withN(expectedEstimatedNewPrice.toString), AttributeAction.PUT),
      "estimatedNewPrice"
    )
    assertEquals(
      update.get("billingPeriod"),
      new AttributeValueUpdate(new AttributeValue().withS(expectedBillingPeriod), AttributeAction.PUT),
      "billingPeriod"
    )
    assertEquals(
      update.get("salesforcePriceRiseId"),
      new AttributeValueUpdate(new AttributeValue().withS(expectedPriceRiseId), AttributeAction.PUT),
      "salesforcePriceRiseId"
    )
    assertEquals(
      update.get("whenSfShowEstimate"),
      new AttributeValueUpdate(
        new AttributeValue()
          .withS(DateTimeFormatter.ISO_DATE_TIME.format(expectedSfShowEstimate.atZone(ZoneOffset.UTC))),
        AttributeAction.PUT
      ),
      "whenSfShowEstimate"
    )
    assertEquals(
      update.get("startDate"),
      new AttributeValueUpdate(new AttributeValue().withS(expectedStartDate.toString), AttributeAction.PUT),
      "startDate"
    )
    assertEquals(
      update.get("newSubscriptionId"),
      new AttributeValueUpdate(new AttributeValue().withS(expectedNewSubscriptionId), AttributeAction.PUT),
      "newSubscriptionId"
    )
    assertEquals(
      update.get("whenAmendmentDone"),
      new AttributeValueUpdate(
        new AttributeValue()
          .withS(formatTimestamp(expectedWhenAmendmentDone)),
        AttributeAction.PUT
      ),
      "whenAmendmentDone"
    )
    assertEquals(
      update.get("whenNotificationSentWrittenToSalesforce"),
      new AttributeValueUpdate(
        new AttributeValue()
          .withS(formatTimestamp(expectedWhenNotificationSentWrittenToSalesforce)),
        AttributeAction.PUT
      ),
      "whenNotificationSentWrittenToSalesforce"
    )
    assertEquals(
      update.get("whenNotificationSent"),
      new AttributeValueUpdate(
        new AttributeValue()
          .withS(formatTimestamp(expectedWhenNotificationSent)),
        AttributeAction.PUT
      ),
      "whenNotificationSent"
    )
  }

  private def formatTimestamp(instant: Instant) = {
    DateTimeFormatter.ISO_DATE_TIME.format(instant.atZone(ZoneOffset.UTC))
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
      Some(new AttributeValueUpdate(new AttributeValue().withS(expectedProcessingStage.value), AttributeAction.PUT)),
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
    assertEquals(insert.get("subscriptionNumber"), new AttributeValue().withS("Subscription-id"))
    assertEquals(insert.get("processingStage"), new AttributeValue().withS("ReadyForEstimation"))
  }
}
