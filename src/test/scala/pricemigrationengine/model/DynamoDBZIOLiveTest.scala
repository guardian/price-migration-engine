package pricemigrationengine.model

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest, QueryResult, UpdateItemResult}
import pricemigrationengine.services.{CohortTable, CohortTableLive, Configuration, ConsoleLogging, DynamoDBDeserialiser, DynamoDBZIO, DynamoDBZIOError, DynamoDBZIOLive}
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.Sink
import zio.{IO, Runtime, ZIO, ZLayer, console}

import scala.jdk.CollectionConverters._

class DynamoDBZIOLiveTest extends munit.FunSuite {
  val stubConfiguration = ZLayer.succeed(
    new Configuration.Service {
      override val config: IO[ConfigurationFailure, Config] =
        IO.succeed(Config("DEV", ZuoraConfig("", "", ""), DynamoDBConfig(None)))
    }
  )

  val stubLogging = console.Console.live >>> ConsoleLogging.impl

  test("DynamoDBZIOLive should get all batches of query results and convert the batches to a stream") {
    def item(id: String) = Map("id" -> new AttributeValue(id)).asJava
    implicit val itemDeserialiser = new DynamoDBDeserialiser[String] {
      def deserialise(value: java.util.Map[String, AttributeValue]) =
        ZIO.fromOption(value.asScala.get("id").map(_.getS))
          .mapError(_ => DynamoDBZIOError(""))
    }

    val queryRequest = new QueryRequest()
    val responseMap = Map(
      queryRequest.clone() -> new QueryResult()
        .withItems(item("id-1"),item("id-2"))
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
        .provideLayer((stubConfiguration ++ stubDynamoDBClient ++ stubLogging) >>> DynamoDBZIOLive.impl)
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
}