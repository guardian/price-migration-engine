package pricemigrationengine.dynamodb

import java.util

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import pricemigrationengine.services.Logging
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

case class DynamoDBZIOError(message: String)

trait DynamoDBSerialiser[A] { def serialise(value: A): java.util.Map[String, AttributeValue] }
trait DynamoDBUpdateSerialiser[A] { def serialise(value: A): java.util.Map[String, AttributeValueUpdate] }
trait DynamoDBDeserialiser[A] { def deserialise(value: java.util.Map[String, AttributeValue]): IO[DynamoDBZIOError, A]  }

object DynamoDBZIO {
  trait Service {
    def query[A](query: QueryRequest)(implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A]
    def update[A, B](table: String, key: A, value: B)
                    (implicit keySerializer: DynamoDBSerialiser[A],
                     valueSerializer: DynamoDBUpdateSerialiser[B]): IO[DynamoDBZIOError, Unit]
  }

  val impl:  ZLayer[DynamoDBClient with Logging, Nothing, DynamoDBZIO] =
    ZLayer.fromFunction { dependencies: DynamoDBClient with Logging =>
      new Service {
        private val logging = dependencies.get[Logging.Service]
        private val amazonDynamoDB = dependencies.get[AmazonDynamoDB]

        override def query[A](
          query: QueryRequest
        )(implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A] =
          recursivelyExecuteQueryUntilAllResultsAreStreamed(query)
            .mapM(deserializer.deserialise)

        private def recursivelyExecuteQueryUntilAllResultsAreStreamed[A](
          query: QueryRequest
        ): ZStream[Any, DynamoDBZIOError, util.Map[String, AttributeValue]] = {
          ZStream.unfoldM(Some(query): Option[QueryRequest]) {
              case Some(queryRequest) =>
                for {
                  queryResult <- sendQueryRequest(queryRequest)
                  _ <- logging.info(s"Received query results for batch with ${queryResult.getCount} items")
                  queryForNextBatch = Option(queryResult.getLastEvaluatedKey)
                    .map(lastEvaluatedKey => queryRequest.withExclusiveStartKey(lastEvaluatedKey))
                } yield Some((queryResult.getItems().asScala, queryForNextBatch))
              case None =>
                ZIO.succeed(None)
            }
            .flatMap(resultList => ZStream.fromIterable(resultList))
        }

        private def sendQueryRequest[A](queryRequest: QueryRequest): ZIO[Any, DynamoDBZIOError, QueryResult] = {
          ZIO.effect {
              logging.info(s"Starting query: $queryRequest")
              amazonDynamoDB.query(queryRequest)
            }
            .mapError(ex => DynamoDBZIOError(s"Failed to execute query $queryRequest : $ex"))
        }

        def update[A, B](table: String, key: A, value: B)
                        (implicit keySerializer: DynamoDBSerialiser[A],
                                  valueSerializer: DynamoDBUpdateSerialiser[B]): IO[DynamoDBZIOError, Unit] = {
          ZIO
            .effect {
              amazonDynamoDB.updateItem(
                new UpdateItemRequest(
                  table,
                  keySerializer.serialise(key),
                  valueSerializer.serialise(value)
                )
              )
              ()
            }
            .mapError(ex => DynamoDBZIOError(s"Failed to write value '$value' to '$table': $ex"))
        }
      }
    }
}
