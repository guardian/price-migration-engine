package pricemigrationengine.services

import java.util

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest, QueryResult, UpdateItemRequest}
import pricemigrationengine.services.DynamoDBZIO.Service
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

object DynamoDBZIOLive {
  val impl: ZLayer[DynamoDBClient with Logging with Configuration, Nothing, DynamoDBZIO] =
    ZLayer.fromFunction { dependencies: DynamoDBClient with Logging with Configuration =>
      new Service {
        override def query[A](
          query: QueryRequest
        )(implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A] =
          recursivelyExecuteQueryUntilAllResultsAreStreamed(query)
            .mapM(deserializer.deserialise)

        private def recursivelyExecuteQueryUntilAllResultsAreStreamed[A](
          query: QueryRequest
        ): ZStream[Any, DynamoDBZIOError, util.Map[String, AttributeValue]] = {
          ZStream.unfoldM(Some(query).asInstanceOf[Option[QueryRequest]]) {
            case Some(queryRequest) =>
              for {
                queryResult <- sendQueryRequest(queryRequest)
                _ <- Logging.info(s"Received query results for batch with ${queryResult.getCount} items")
                queryForNextBatch = Option(queryResult.getLastEvaluatedKey)
                  .map(lastEvaluatedKey => queryRequest.withExclusiveStartKey(lastEvaluatedKey))
              } yield Some((queryResult.getItems().asScala, queryForNextBatch))
            case None =>
              ZIO.succeed(None)
          }
            .flatMap(resultList => ZStream.fromIterable(resultList))
        }.provide(dependencies)

        private def sendQueryRequest[A](queryRequest: QueryRequest): ZIO[Any, DynamoDBZIOError, QueryResult] = {
          for {
            _ <- Logging.info(s"Starting query: $queryRequest")
            results <- DynamoDBClient.query(queryRequest)
              .mapError(ex => DynamoDBZIOError(s"Failed to execute query $queryRequest : $ex"))
          } yield results
        }.provide(dependencies)

        def update[A, B](table: String, key: A, value: B)
                        (implicit keySerializer: DynamoDBSerialiser[A],
                         valueSerializer: DynamoDBUpdateSerialiser[B]): IO[DynamoDBZIOError, Unit] =
          DynamoDBClient.updateItem(
            new UpdateItemRequest(
              table,
              keySerializer.serialise(key),
              valueSerializer.serialise(value)
            )
          ).bimap(
            ex => DynamoDBZIOError(s"Failed to write value '$value' to '$table': $ex"),
            _ => ()
          ).provide(dependencies)
      }
    }
}