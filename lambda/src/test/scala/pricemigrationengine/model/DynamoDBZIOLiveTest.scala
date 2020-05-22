package pricemigrationengine.model

import java.util

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import pricemigrationengine.services._
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.Sink
import zio.{ZIO, ZLayer, console}

import scala.jdk.CollectionConverters._

class DynamoDBZIOLiveTest extends munit.FunSuite {
  val stubLogging = console.Console.live >>> ConsoleLogging.impl

  test("DynamoDBZIOLive should get all batches of query results and convert the batches to a stream") {
    def item(id: String) = Map("id" -> new AttributeValue(id)).asJava

    implicit val itemDeserialiser = new DynamoDBDeserialiser[String] {
      def deserialise(value: java.util.Map[String, AttributeValue]) =
        ZIO.fromOption(value.asScala.get("id").map(_.getS)).orElseFail(DynamoDBZIOError(""))
    }

    val queryRequest = new QueryRequest()
    val responseMap = Map(
      queryRequest.clone() -> new QueryResult()
        .withItems(item("id-1"), item("id-2"))
        .withLastEvaluatedKey(item("id-2")),
      queryRequest.clone().withExclusiveStartKey(item("id-2")) -> new QueryResult()
        .withItems(item("id-3"))
    )
    val stubDynamoDBClient = ZLayer.succeed[AmazonDynamoDB](
      new AmazonDynamoDBSubBase {
        override def query(queryRequest: QueryRequest): QueryResult = {
          responseMap(queryRequest)
        }
      }
    )

    default.unsafeRunSync(
      DynamoDBZIO
        .query(queryRequest)
        .provideLayer((stubDynamoDBClient ++ stubLogging) >>> DynamoDBZIOLive.impl)
    ) match {
      case Success(results) =>
        assertEquals(
          default.unsafeRunSync(results.run(Sink.collectAll[String])),
          Success(List("id-1", "id-2", "id-3"))
        )
      case failure =>
        fail(s"Query returned failure $failure")
    }
  }

  test("DynamoDBZIOLive serialize key and values and update in dynamodb") {
    implicit val keySerialiser = new DynamoDBSerialiser[String] {
      override def serialise(key: String): util.Map[String, AttributeValue] =
        Map("key" -> new AttributeValue().withS(key)).asJava
    }

    implicit val updateSerialiser = new DynamoDBUpdateSerialiser[String] {
      override def serialise(value: String): util.Map[String, AttributeValueUpdate] =
        Map("value" -> new AttributeValueUpdate(new AttributeValue().withS(value), AttributeAction.PUT)).asJava
    }

    var receivedUpdateItemRequest: Option[UpdateItemRequest] = None

    val stubDynamoDBClient = ZLayer.succeed[AmazonDynamoDB](
      new AmazonDynamoDBSubBase {
        override def updateItem(updateItemRequest: UpdateItemRequest): UpdateItemResult = {
          receivedUpdateItemRequest = Some(updateItemRequest)
          new UpdateItemResult()
        }
      }
    )

    assertEquals(
      default.unsafeRunSync(
        DynamoDBZIO
          .update("a-table", "key", "value")
          .provideLayer((stubDynamoDBClient ++ stubLogging) >>> DynamoDBZIOLive.impl)
      ),
      Success(())
    )

    assertEquals(
      receivedUpdateItemRequest,
      Some(
        new UpdateItemRequest()
          .withTableName("a-table")
          .withKey(
            Map("key" -> new AttributeValue().withS("key")).asJava
          )
          .withAttributeUpdates(
            Map("value" -> new AttributeValueUpdate(new AttributeValue().withS("value"), AttributeAction.PUT)).asJava
          )
      )
    )
  }
}
