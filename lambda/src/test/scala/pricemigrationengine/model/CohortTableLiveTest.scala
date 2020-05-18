package pricemigrationengine.model

import java.time.{Instant, LocalDate}

import com.amazonaws.services.dynamodbv2.model.{AttributeAction, AttributeValue, AttributeValueUpdate, QueryRequest}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.services._
import zio.Exit.Success
import zio.stream.{Sink, ZStream}
import zio.{IO, Runtime, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

class CohortTableLiveTest extends munit.FunSuite {
  val stubConfiguration = ZLayer.succeed(
    new Configuration.Service {
      override val config: IO[ConfigurationFailure, Config] =
        IO.succeed(Config(ZuoraConfig("", "", ""), DynamoDBConfig(None), "DEV", LocalDate.now))
    }
  )

  test("Query the PriceMigrationEngine with the correct filter and parse the results") {
    val item1 = CohortItem("subscription-1")
    val item2 = CohortItem("subscription-2")

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

        override def update[A, B](table: String, key: A, value: B)(
            implicit keySerializer: DynamoDBSerialiser[A],
            valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] = ???
      }
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        for {
          result <- CohortTable
            .fetch(ReadyForEstimation, 10)
            .provideLayer(stubConfiguration ++ stubDynamoDBZIO >>> CohortTableLive.impl)
          resultList <- result.run(Sink.collectAll[CohortItem])
          _ = assertEquals(resultList, List(item1, item2))
        } yield ()
      ),
      Success(())
    )

    assertEquals(receivedRequest.get.getTableName, "PriceMigrationEngineDEV")
    assertEquals(receivedRequest.get.getIndexName, "ProcessingStageIndex")
    assertEquals(receivedRequest.get.getKeyConditionExpression, "processingStage = :processingStage")
    assertEquals(
      receivedRequest.get.getExpressionAttributeValues,
      Map(":processingStage" -> new AttributeValue().withS("ReadyForEstimation")).asJava
    )
    assertEquals(
      Runtime.default.unsafeRunSync(
        receivedDeserialiser.get.deserialise(
          Map("subscriptionNumber" -> new AttributeValue().withS("subscription-number")).asJava
        )
      ),
      Success(CohortItem("subscription-number"))
    )
  }

  test("Update the PriceMigrationEngine table and serialise the update correctly") {
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
          .provideLayer(stubConfiguration ++ stubDynamoDBZIO >>> CohortTableLive.impl)
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
      whenEstimationDone.isAfter(now.minusSeconds(100)) && whenEstimationDone.isBefore(now),
      "whenEstimationDone"
    )
  }
}
