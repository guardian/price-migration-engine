package pricemigrationengine.model

import java.util

import com.amazonaws.services.dynamodbv2.model._
import pricemigrationengine.TestLogging
import pricemigrationengine.services._
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.Sink
import zio.{Chunk, Task, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

class DynamoDBZIOLiveTest extends munit.FunSuite {
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
    val stubDynamoDBClient = ZLayer.succeed(
      new DynamoDBClient.Service {
        def query(queryRequest: QueryRequest): Task[QueryResult] = Task.succeed(responseMap(queryRequest))

        def scan(scanRequest: ScanRequest): Task[ScanResult] = ???

        def updateItem(updateRequest: UpdateItemRequest): Task[UpdateItemResult] = ???

        def createItem(createRequest: PutItemRequest, keyName: String): Task[PutItemResult] = ???

        def describeTable(tableName: String): Task[DescribeTableResult] = ???

        def createTable(request: CreateTableRequest): Task[CreateTableResult] = ???

        def updateContinuousBackups(request: UpdateContinuousBackupsRequest): Task[UpdateContinuousBackupsResult] = ???
      }
    )

    default.unsafeRunSync(
      DynamoDBZIO
        .query(queryRequest)
        .provideLayer((stubDynamoDBClient ++ TestLogging.logging) >>> DynamoDBZIOLive.impl)
    ) match {
      case Success(results) =>
        assertEquals(
          default.unsafeRunSync(results.run(Sink.collectAll[String])),
          Success(Chunk("id-1", "id-2", "id-3"))
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

    val stubDynamoDBClient = ZLayer.succeed(
      new DynamoDBClient.Service {
        def query(queryRequest: QueryRequest): Task[QueryResult] = ???

        def scan(scanRequest: ScanRequest): Task[ScanResult] = ???

        def updateItem(updateItemRequest: UpdateItemRequest): Task[UpdateItemResult] =
          Task.succeed {
            receivedUpdateItemRequest = Some(updateItemRequest)
            new UpdateItemResult()
          }

        def createItem(createRequest: PutItemRequest, keyName: String): Task[PutItemResult] = ???

        def describeTable(tableName: String): Task[DescribeTableResult] = ???

        def createTable(request: CreateTableRequest): Task[CreateTableResult] = ???

        def updateContinuousBackups(request: UpdateContinuousBackupsRequest): Task[UpdateContinuousBackupsResult] = ???
      }
    )

    assertEquals(
      default.unsafeRunSync(
        DynamoDBZIO
          .update("a-table", "key", "value")
          .provideLayer((stubDynamoDBClient ++ TestLogging.logging) >>> DynamoDBZIOLive.impl)
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
