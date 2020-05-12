package pricemigrationengine.model

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.services._
import zio.Exit.Success
import zio.stream.{Sink, ZStream}
import zio.{IO, Runtime, ZLayer}

import scala.jdk.CollectionConverters._

class CohortTableLiveTest extends munit.FunSuite {
  test("Query the PriceMigrationEngine with the correct filter and parse the results") {
    val stubConfiguration = ZLayer.succeed(
      new Configuration.Service {
        override val config: IO[ConfigurationFailure, Config] =
          IO.succeed(Config("DEV", ZuoraConfig("", "", ""), DynamoDBConfig(None)))
      }
    )

    val item1 = CohortItem("subscription-1")
    val item2 = CohortItem("subscription-2")

    val stubDynamoDBZIO = ZLayer.succeed(
      new DynamoDBZIO.Service {
        override def query[A](query: QueryRequest)
                             (implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A] = {

          assertEquals(query.getTableName, "PriceMigrationEngineDEV")
          assertEquals(query.getIndexName, "ProcessingStageIndex")
          assertEquals(query.getKeyConditionExpression, "processingStage = :processingStage")
          assertEquals(
            query.getExpressionAttributeValues,
            Map(":processingStage" -> new AttributeValue().withS("ReadyForEstimation")).asJava
          )
          assertEquals(
            Runtime.default.unsafeRunSync(
              deserializer.deserialise(
                Map("subscriptionNumber" -> new AttributeValue().withS("subscription-number")).asJava
              )
            ),
            Success(CohortItem("subscription-number").asInstanceOf[A])
          )
          ZStream(item1, item2).mapM(item => IO.effect(item.asInstanceOf[A]).mapError(_ => DynamoDBZIOError("")))
        }

        override def update[A, B](table: String, key: A, value: B)
                                 (implicit keySerializer: DynamoDBSerialiser[A],
                                  valueSerializer: DynamoDBUpdateSerialiser[B]): IO[DynamoDBZIOError, Unit] = ???
      }
    )

    assertEquals(
      Runtime.default.unsafeRunSync(
        (
          for {
            result <- CohortTable
              .fetch(ReadyForEstimation, 10)
              .provideLayer(stubConfiguration ++ stubDynamoDBZIO >>> CohortTableLive.impl)
            resultList <- result.run(Sink.collectAll[CohortItem])
            _ = assertEquals(resultList, List(item1, item2))
          } yield ()
          )
      ),
      Success(())
    )
  }
}
