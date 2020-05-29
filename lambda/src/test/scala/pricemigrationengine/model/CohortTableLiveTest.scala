package pricemigrationengine.model

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset}

import com.amazonaws.services.dynamodbv2.model.{AttributeAction, AttributeValue, AttributeValueUpdate, QueryRequest}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.services._
import zio.Exit.Success
import zio.stream.{Sink, ZStream}
import zio.{IO, Runtime, ZIO, ZLayer}

import scala.jdk.CollectionConverters._
import scala.util.Random

class CohortTableLiveTest extends munit.FunSuite {
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

  test("Query the PriceMigrationEngine with the correct filter and parse the results") {
    val item1 = CohortItem("subscription-1", ReadyForEstimation)
    val item2 = CohortItem("subscription-2", ReadyForEstimation)

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

        override def update[A, B](table: String, key: A, value: B)
                                 (implicit keySerializer: DynamoDBSerialiser[A],
                                  valueSerializer: DynamoDBUpdateSerialiser[B]): IO[DynamoDBZIOError, Unit] = ???

        override def put[A](table: String, value: A)
                           (implicit valueSerializer: DynamoDBSerialiser[A]): IO[DynamoDBZIOError, Unit] = ???
      }
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        for {
          result <- CohortTable
            .fetch(ReadyForEstimation)
            .provideLayer(
              stubCohortTableConfiguration ++ stubStageConfiguration++ stubDynamoDBZIO ++ ConsoleLogging.impl >>>
                CohortTableLive.impl
            )
          resultList <- result.run(Sink.collectAll[CohortItem])
          _ = assertEquals(resultList, List(item1, item2))
        } yield ()
      ),
      Success(())
    )

    assertEquals(receivedRequest.get.getTableName, "PriceMigrationEngineDEV")
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
            "subscriptionNumber" -> new AttributeValue().withS("subscription-number"),
            "processingStage" -> new AttributeValue().withS("ReadyForEstimation")
          ).asJava
        )
      ),
      Success(CohortItem("subscription-number", ReadyForEstimation))
    )
  }

  test("Update the PriceMigrationEngine table and serialise the estimation update correctly") {
    var tableUpdated: Option[String] = None
    var receivedKey: Option[CohortTableKey] = None
    var receivedUpdate: Option[EstimationResult] = None
    var receivedKeySerialiser: Option[DynamoDBSerialiser[CohortTableKey]] = None
    var receivedValueSerialiser: Option[DynamoDBUpdateSerialiser[EstimationResult]] = None

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO.Service {
        override def query[A](query: QueryRequest)(
            implicit deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???

        override def update[A, B](table: String, key: A, value: B)(
            implicit keySerializer: DynamoDBSerialiser[A],
            valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = {
          tableUpdated = Some(table)
          receivedKey = Some(key.asInstanceOf[CohortTableKey])
          receivedUpdate = Some(value.asInstanceOf[EstimationResult])
          receivedKeySerialiser = Some(keySerializer.asInstanceOf[DynamoDBSerialiser[CohortTableKey]])
          receivedValueSerialiser = Some(valueSerializer.asInstanceOf[DynamoDBUpdateSerialiser[EstimationResult]])
          ZIO.effect(()).orElseFail(DynamoDBZIOError(""))
        }

        override def put[A](table: String, value: A)
                           (implicit valueSerializer: DynamoDBSerialiser[A]): IO[DynamoDBZIOError, Unit] = ???
      }
    )

    val estimationResult = EstimationResult(
      subscriptionName = "subscription-name",
      expectedStartDate = LocalDate.of(2020, 1, 1),
      currency = "GBP",
      oldPrice = 1.0,
      estimatedNewPrice = 2.0,
      billingPeriod = "Quarter"
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        CohortTable
          .update(estimationResult)
          .provideLayer(
            stubCohortTableConfiguration ++ stubStageConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl >>>
            CohortTableLive.impl
          )
      ),
      Success(())
    )

    assertEquals(tableUpdated.get, "PriceMigrationEngineDEV")
    assertEquals(receivedKey.get.subscriptionNumber, "subscription-name")
    assertEquals(
      receivedKeySerialiser.get.serialise(receivedKey.get),
      Map(
        "subscriptionNumber" -> new AttributeValue().withS("subscription-name")
      ).asJava
    )
    val update = receivedValueSerialiser.get.serialise(receivedUpdate.get)
    assertEquals(
      update.get("processingStage"),
      new AttributeValueUpdate(new AttributeValue().withS("EstimationComplete"), AttributeAction.PUT),
      "processingStage"
    )
    assertEquals(
      update.get("expectedStartDate"),
      new AttributeValueUpdate(new AttributeValue().withS("2020-01-01"), AttributeAction.PUT),
      "expectedStartDate"
    )
    assertEquals(
      update.get("currency"),
      new AttributeValueUpdate(new AttributeValue().withS("GBP"), AttributeAction.PUT),
      "currency"
    )
    assertEquals(
      update.get("oldPrice"),
      new AttributeValueUpdate(new AttributeValue().withN("1.0"), AttributeAction.PUT),
      "oldPrice"
    )
    assertEquals(
      update.get("estimatedNewPrice"),
      new AttributeValueUpdate(new AttributeValue().withN("2.0"), AttributeAction.PUT),
      "estimatedNewPrice"
    )
    assertEquals(
      update.get("billingPeriod"),
      new AttributeValueUpdate(new AttributeValue().withS("Quarter"), AttributeAction.PUT),
      "billingPeriod"
    )
    val now = Instant.now
    val whenEstimationDone = Instant.parse(update.get("whenEstimationDone").getValue.getS)
    assert(
      whenEstimationDone.isAfter(now.minusSeconds(100)) && (!whenEstimationDone.isAfter(now)),
      "whenEstimationDone"
    )
  }

  test("Update the cohort table with an amendment result") {
    var tableUpdated: Option[String] = None
    var receivedKey: Option[CohortTableKey] = None
    var receivedUpdate: Option[AmendmentResult] = None
    var receivedKeySerialiser: Option[DynamoDBSerialiser[CohortTableKey]] = None
    var receivedValueSerialiser: Option[DynamoDBUpdateSerialiser[AmendmentResult]] = None

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO.Service {
        override def query[A](query: QueryRequest)(
            implicit deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???

        override def update[A, B](table: String, key: A, value: B)(
            implicit keySerializer: DynamoDBSerialiser[A],
            valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = {
          tableUpdated = Some(table)
          receivedKey = Some(key.asInstanceOf[CohortTableKey])
          receivedUpdate = Some(value.asInstanceOf[AmendmentResult])
          receivedKeySerialiser = Some(keySerializer.asInstanceOf[DynamoDBSerialiser[CohortTableKey]])
          receivedValueSerialiser = Some(valueSerializer.asInstanceOf[DynamoDBUpdateSerialiser[AmendmentResult]])
          ZIO.effect(()).orElseFail(DynamoDBZIOError(""))
        }

        override def put[A](table: String, value: A)
                           (implicit valueSerializer: DynamoDBSerialiser[A]): IO[DynamoDBZIOError, Unit] = ???
      }
    )

    val amendmentResult = AmendmentResult(
      subscriptionName = "subscription-name",
      startDate = LocalDate.of(2020, 1, 1),
      newPrice = 2.01,
      newSubscriptionId = "id"
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        CohortTable
          .update(amendmentResult)
          .provideLayer(
            stubStageConfiguration ++ stubCohortTableConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl
              >>> CohortTableLive.impl
          )
      ),
      Success(())
    )

    assertEquals(tableUpdated.get, "PriceMigrationEngineDEV")
    assertEquals(receivedKey.get.subscriptionNumber, "subscription-name")
    assertEquals(
      receivedKeySerialiser.get.serialise(receivedKey.get),
      Map(
        "subscriptionNumber" -> new AttributeValue().withS("subscription-name")
      ).asJava
    )
    val update = receivedValueSerialiser.get.serialise(receivedUpdate.get)
    assertEquals(
      update.get("processingStage"),
      new AttributeValueUpdate(new AttributeValue().withS("AmendmentComplete"), AttributeAction.PUT),
      "processingStage"
    )
    assertEquals(
      update.get("startDate"),
      new AttributeValueUpdate(new AttributeValue().withS("2020-01-01"), AttributeAction.PUT),
      "expectedStartDate"
    )
    assertEquals(
      update.get("newPrice"),
      new AttributeValueUpdate(new AttributeValue().withN("2.01"), AttributeAction.PUT),
      "newPrice"
    )
    assertEquals(
      update.get("newSubscriptionId"),
      new AttributeValueUpdate(new AttributeValue().withS("id"), AttributeAction.PUT),
      "newPrice"
    )
    val now = Instant.now
    val whenEstimationDone = Instant.parse(update.get("whenAmendmentDone").getValue.getS)
    assert(
      whenEstimationDone.isAfter(now.minusSeconds(100)) && (!whenEstimationDone.isAfter(now)),
      "whenAmendmentDone"
    )
  }

  test("Update the PriceMigrationEngine table and serialise the price rise update correctly") {
    var tableUpdated: Option[String] = None
    var receivedKey: Option[CohortTableKey] = None
    var receivedUpdate: Option[SalesforcePriceRiseCreationDetails] = None
    var receivedKeySerialiser: Option[DynamoDBSerialiser[CohortTableKey]] = None
    var receivedValueSerialiser: Option[DynamoDBUpdateSerialiser[SalesforcePriceRiseCreationDetails]] = None

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO.Service {
        override def query[A](query: QueryRequest)(
          implicit deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???

        override def update[A, B](table: String, key: A, value: B)(
          implicit keySerializer: DynamoDBSerialiser[A],
          valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = {
          tableUpdated = Some(table)
          receivedKey = Some(key.asInstanceOf[CohortTableKey])
          receivedUpdate = Some(value.asInstanceOf[SalesforcePriceRiseCreationDetails])
          receivedKeySerialiser = Some(keySerializer.asInstanceOf[DynamoDBSerialiser[CohortTableKey]])
          receivedValueSerialiser = Some(valueSerializer.asInstanceOf[DynamoDBUpdateSerialiser[SalesforcePriceRiseCreationDetails]])
          ZIO.effect(()).orElseFail(DynamoDBZIOError(""))
        }

        override def put[A](table: String, value: A)
                           (implicit valueSerializer: DynamoDBSerialiser[A]): IO[DynamoDBZIOError, Unit] = ???
      }
    )

    val expectedTimestamp = "2020-05-21T15:16:37Z"

    val salesforcePriceRiseCreationResult = SalesforcePriceRiseCreationDetails(
      id = "price-rise-id",
      Instant.parse(expectedTimestamp)
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        CohortTable
          .update("subscription-name", salesforcePriceRiseCreationResult)
          .provideLayer(
            stubStageConfiguration ++ stubCohortTableConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl >>>
            CohortTableLive.impl
          )
      ),
      Success(())
    )

    assertEquals(tableUpdated.get, "PriceMigrationEngineDEV")
    assertEquals(receivedKey.get.subscriptionNumber, "subscription-name")
    assertEquals(
      receivedKeySerialiser.get.serialise(receivedKey.get),
      Map(
        "subscriptionNumber" -> new AttributeValue().withS("subscription-name")
      ).asJava
    )
    val update = receivedValueSerialiser.get.serialise(receivedUpdate.get)
    assertEquals(
      update.get("processingStage"),
      new AttributeValueUpdate(new AttributeValue().withS("SalesforcePriceRiceCreationComplete"), AttributeAction.PUT),
      "processingStage"
    )
    assertEquals(
      update.get("salesforcePriceRiseId"),
      new AttributeValueUpdate(new AttributeValue().withS("price-rise-id"), AttributeAction.PUT),
      "salesforcePriceRiseId"
    )
    assertEquals(
      update.get("whenSfShowEstimate"),
      new AttributeValueUpdate(new AttributeValue().withS(expectedTimestamp), AttributeAction.PUT),
      "salesforcePriceRiseId"
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
        override def query[A](query: QueryRequest)(
          implicit deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???

        override def update[A, B](table: String, key: A, value: B)(
          implicit keySerializer: DynamoDBSerialiser[A],
          valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = {
          tableUpdated = Some(table)
          receivedKey = Some(key.asInstanceOf[CohortTableKey])
          receivedUpdate = Some(value.asInstanceOf[CohortItem])
          receivedKeySerialiser = Some(keySerializer.asInstanceOf[DynamoDBSerialiser[CohortTableKey]])
          receivedValueSerialiser = Some(valueSerializer.asInstanceOf[DynamoDBUpdateSerialiser[CohortItem]])
          ZIO.effect(()).orElseFail(DynamoDBZIOError(""))
        }

        override def put[A](table: String, value: A)
                           (implicit valueSerializer: DynamoDBSerialiser[A]): IO[DynamoDBZIOError, Unit] = ???
      }
    )

    val expectedSubscriptionId = "subscription-id"
    val expectedProcessingStage = ReadyForEstimation
    val expectedStartDate = LocalDate.now.plusDays(Random.nextInt(365))
    val expectedCurrency = "GBP"
    val expectedOldPrice = Random.nextDouble()
    val expectedNewPrice = Random.nextDouble()
    val expectedEstimatedNewPrice = Random.nextDouble()
    val expectedBillingPeriod = "Monthly"
    val expectedWhenEstimationDone =  Instant.ofEpochMilli(Random.nextLong())
    val expectedPriceRiseId = "price-rise-id"
    val expectedSfShowEstimate =  Instant.ofEpochMilli(Random.nextLong())
    val expectedNewSuscriptionId = "new-sub-id"
    val expectedWhenAmmendmentDone =  Instant.ofEpochMilli(Random.nextLong())

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
      newSubscriptionId = Some(expectedNewSuscriptionId),
      whenAmendmentDone = Some(expectedWhenAmmendmentDone)
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        CohortTable
          .update(cohortItem)
          .provideLayer(
            stubStageConfiguration ++ stubCohortTableConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl >>>
            CohortTableLive.impl
          )
      ),
      Success(())
    )

    assertEquals(tableUpdated.get, "PriceMigrationEngineDEV")
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
      new AttributeValueUpdate(new AttributeValue().withS(DateTimeFormatter.ISO_DATE_TIME.format(expectedSfShowEstimate.atZone(ZoneOffset.UTC))), AttributeAction.PUT),
      "whenSfShowEstimate"
    )
    assertEquals(
      update.get("startDate"),
      new AttributeValueUpdate(new AttributeValue().withS(expectedStartDate.toString), AttributeAction.PUT),
      "startDate"
    )
    assertEquals(
      update.get("newSubscriptionId"),
      new AttributeValueUpdate(new AttributeValue().withS(expectedNewSuscriptionId), AttributeAction.PUT),
      "newSubscriptionId"
    )
    assertEquals(
      update.get("whenAmendmentDone"),
      new AttributeValueUpdate(new AttributeValue().withS(expectedWhenAmmendmentDone.toString), AttributeAction.PUT),
      "whenAmendmentDone"
    )
  }

  test("Update the PriceMigrationEngine table and serialise the CohortItem with missing optional values correctly") {
    var receivedUpdate: Option[CohortItem] = None
    var receivedValueSerialiser: Option[DynamoDBUpdateSerialiser[CohortItem]] = None

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO.Service {
        override def query[A](query: QueryRequest)(
          implicit deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???

        override def update[A, B](table: String, key: A, value: B)(
          implicit keySerializer: DynamoDBSerialiser[A],
          valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = {
          receivedValueSerialiser = Some(valueSerializer.asInstanceOf[DynamoDBUpdateSerialiser[CohortItem]])
          receivedUpdate = Some(value.asInstanceOf[CohortItem])
          ZIO.effect(()).orElseFail(DynamoDBZIOError(""))
        }

        override def put[A](table: String, value: A)
                           (implicit valueSerializer: DynamoDBSerialiser[A]): IO[DynamoDBZIOError, Unit] = ???
      }
    )

    val expectedSubscriptionId = "subscription-id"
    val expectedProcessingStage = ReadyForEstimation

    val cohortItem = CohortItem(
      subscriptionName = expectedSubscriptionId,
      processingStage = expectedProcessingStage,
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        CohortTable
          .update(cohortItem)
          .provideLayer(
            stubStageConfiguration ++ stubCohortTableConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl >>>
            CohortTableLive.impl
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
    assertEquals(update.get("newPrice"),None, "newPrice")
    assertEquals(update.get("estimatedNewPrice"), None, "estimatedNewPrice")
    assertEquals(update.get("billingPeriod"), None, "billingPeriod")
    assertEquals(update.get("salesforcePriceRiseId"), None, "salesforcePriceRiseId")
    assertEquals(update.get("whenSfShowEstimate"), None, "whenSfShowEstimate")
    assertEquals(update.get("startDate"), None, "startDate")
    assertEquals(update.get("newSubscriptionId"), None, "newSubscriptionId")
    assertEquals(update.get("whenAmendmentDone"), None, "whenAmendmentDone")
  }

  test("Create the PriceMigrationEngine table and serialise the subscription correctly") {
    var tableUpdated: Option[String] = None
    var receivedInsert: Option[CohortItem] = None
    var receivedSerialiser: Option[DynamoDBSerialiser[CohortItem]] = None

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO.Service {
        override def query[A](query: QueryRequest)(
          implicit deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = ???

        override def update[A, B](table: String, key: A, value: B)(
          implicit keySerializer: DynamoDBSerialiser[A],
          valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = ???

        override def put[A](table: String, value: A)
                           (implicit valueSerializer: DynamoDBSerialiser[A]): IO[DynamoDBZIOError, Unit] = {
          tableUpdated = Some(table)
          receivedInsert = Some(value.asInstanceOf[CohortItem])
          receivedSerialiser = Some(valueSerializer.asInstanceOf[DynamoDBSerialiser[CohortItem]])
          ZIO.effect(()).orElseFail(DynamoDBZIOError(""))
        }
      }
    )

    val cohortItem = CohortItem("Subscription-id", ReadyForEstimation)

    assertEquals(
      Runtime.default.unsafeRunSync(
        CohortTable
          .put(cohortItem)
          .provideLayer(
            stubStageConfiguration ++ stubCohortTableConfiguration ++ stubDynamoDBZIO ++ ConsoleLogging.impl >>>
              CohortTableLive.impl
          )
      ),
      Success(())
    )

    assertEquals(tableUpdated.get, "PriceMigrationEngineDEV")
    val insert = receivedSerialiser.get.serialise(receivedInsert.get)
    assertEquals(insert.get("subscriptionNumber"), new AttributeValue().withS("Subscription-id"))
    assertEquals(insert.get("processingStage"), new AttributeValue().withS("ReadyForEstimation"))
  }
}
