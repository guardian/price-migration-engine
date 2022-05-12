package pricemigrationengine.model

import pricemigrationengine.TestLogging
import pricemigrationengine.services._
import software.amazon.awssdk.services.dynamodb.model.AttributeAction.PUT
import software.amazon.awssdk.services.dynamodb.model._
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.ZSink
import zio.{Chunk, Task, ZIO, ZLayer}

import java.util
import scala.jdk.CollectionConverters._

class DynamoDBZIOLiveTest extends munit.FunSuite {
  test("DynamoDBZIOLive should get all batches of query results and convert the batches to a stream") {
    def item(id: String) = Map("id" -> AttributeValue.builder.s(id).build()).asJava

    implicit val itemDeserialiser = new DynamoDBDeserialiser[String] {
      def deserialise(value: java.util.Map[String, AttributeValue]) =
        ZIO.fromOption(value.asScala.get("id").map(_.s)).orElseFail(DynamoDBZIOError(""))
    }

    val queryRequest = QueryRequest.builder.build()
    val responseMap = Map(
      queryRequest -> QueryResponse.builder.items(item("id-1"), item("id-2")).lastEvaluatedKey(item("id-2")).build(),
      queryRequest.copy(x => x.exclusiveStartKey(item("id-2"))) -> QueryResponse.builder.items(item("id-3")).build()
    )
    val stubDynamoDBClient = ZLayer.succeed(
      new DynamoDBClient {
        def query(queryRequest: QueryRequest): Task[QueryResponse] = ZIO.succeed(responseMap(queryRequest))

        def scan(scanRequest: ScanRequest): Task[ScanResponse] = ???

        def updateItem(updateRequest: UpdateItemRequest): Task[UpdateItemResponse] = ???

        def createItem(createRequest: PutItemRequest, keyName: String): Task[PutItemResponse] = ???

        def describeTable(tableName: String): Task[DescribeTableResponse] = ???

        def createTable(request: CreateTableRequest): Task[CreateTableResponse] = ???

        def updateContinuousBackups(request: UpdateContinuousBackupsRequest): Task[UpdateContinuousBackupsResponse] =
          ???
      }
    )

    default.unsafeRunSync(
      DynamoDBZIO
        .query(queryRequest)
        .provideLayer((stubDynamoDBClient ++ TestLogging.logging) >>> DynamoDBZIOLive.impl)
    ) match {
      case Success(results) =>
        assertEquals(
          default.unsafeRunSync(results.run(ZSink.collectAll[String])),
          Success(Chunk("id-1", "id-2", "id-3"))
        )
      case failure =>
        fail(s"Query returned failure $failure")
    }
  }

  test("DynamoDBZIOLive serialize key and values and update in dynamodb") {
    implicit val keySerialiser = new DynamoDBSerialiser[String] {
      override def serialise(key: String): util.Map[String, AttributeValue] =
        Map("key" -> AttributeValue.builder.s(key).build()).asJava
    }

    implicit val updateSerialiser = new DynamoDBUpdateSerialiser[String] {
      override def serialise(value: String): util.Map[String, AttributeValueUpdate] =
        Map(
          "value" -> AttributeValueUpdate.builder
            .value(AttributeValue.builder.s(value).build())
            .action(PUT)
            .build()
        ).asJava
    }

    var receivedUpdateItemRequest: Option[UpdateItemRequest] = None

    val stubDynamoDBClient = ZLayer.succeed(
      new DynamoDBClient {

        override def query(queryRequest: QueryRequest): Task[QueryResponse] = ???

        override def scan(scanRequest: ScanRequest): Task[ScanResponse] = ???

        override def updateItem(updateItemRequest: UpdateItemRequest): Task[UpdateItemResponse] =
          ZIO.succeed {
            receivedUpdateItemRequest = Some(updateItemRequest)
            UpdateItemResponse.builder.build()
          }

        override def createItem(createRequest: PutItemRequest, keyName: String): Task[PutItemResponse] = ???

        override def describeTable(tableName: String): Task[DescribeTableResponse] = ???

        override def createTable(request: CreateTableRequest): Task[CreateTableResponse] = ???

        override def updateContinuousBackups(
            request: UpdateContinuousBackupsRequest
        ): Task[UpdateContinuousBackupsResponse] =
          ???
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
        UpdateItemRequest.builder
          .tableName("a-table")
          .key(
            Map("key" -> AttributeValue.builder.s("key").build()).asJava
          )
          .attributeUpdates(
            Map(
              "value" -> AttributeValueUpdate.builder
                .value(AttributeValue.builder.s("value").build())
                .action(PUT)
                .build()
            ).asJava
          )
          .build()
      )
    )
  }
}
