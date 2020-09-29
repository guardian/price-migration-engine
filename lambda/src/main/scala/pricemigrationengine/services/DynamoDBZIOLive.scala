package pricemigrationengine.services

import java.util

import com.amazonaws.services.dynamodbv2.model._
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

object DynamoDBZIOLive {
  val impl: ZLayer[DynamoDBClient with Logging, Nothing, DynamoDBZIO] =
    ZLayer.fromFunction { dependencies: DynamoDBClient with Logging =>
      new DynamoDBZIO.Service {
        override def query[A](
            query: QueryRequest
        )(implicit deserializer: DynamoDBDeserialiser[A]): ZStream[Any, DynamoDBZIOError, A] =
          recursivelyExecuteQueryUntilAllResultsAreStreamed(query)
            .mapM(deserializer.deserialise)

        private def recursivelyExecuteQueryUntilAllResultsAreStreamed[A](
            query: QueryRequest
        ): ZStream[Any, DynamoDBZIOError, util.Map[String, AttributeValue]] = {
          ZStream
            .unfoldM(Some(query).asInstanceOf[Option[QueryRequest]]) {
              case Some(queryRequest) =>
                for {
                  queryResult <- sendQueryRequest(queryRequest)
                  _ <- Logging.info(s"Received query results for batch with ${queryResult.getCount} items")
                  queryForNextBatch = Option(queryResult.getLastEvaluatedKey)
                    .map(lastEvaluatedKey => queryRequest.clone().withExclusiveStartKey(lastEvaluatedKey))
                } yield Some((queryResult.getItems().asScala, queryForNextBatch))
              case None =>
                ZIO.none
            }
            .flatMap(resultList => ZStream.fromIterable(resultList))
        }.provide(dependencies)

        private def sendQueryRequest[A](queryRequest: QueryRequest): ZIO[Any, DynamoDBZIOError, QueryResult] = {
          for {
            _ <- Logging.info(s"Starting query: $queryRequest")
            results <-
              DynamoDBClient
                .query(queryRequest)
                .mapError(ex => DynamoDBZIOError(s"Failed to execute query $queryRequest : $ex"))
          } yield results
        }.provide(dependencies)

        override def scan[A](query: ScanRequest)(implicit
            deserializer: DynamoDBDeserialiser[A]
        ): ZStream[Any, DynamoDBZIOError, A] = {
          recursivelyExecuteScanUntilAllResultsAreStreamed(query)
            .mapM(deserializer.deserialise)
        }

        private def recursivelyExecuteScanUntilAllResultsAreStreamed[A](
            query: ScanRequest
        ): ZStream[Any, DynamoDBZIOError, util.Map[String, AttributeValue]] = {
          ZStream
            .unfoldM(Some(query).asInstanceOf[Option[ScanRequest]]) {
              case Some(queryRequest) =>
                for {
                  scanResult <- sendScanRequest(queryRequest)
                  _ <- Logging.info(s"Received query results for batch with ${scanResult.getCount} items")
                  queryForNextBatch = Option(scanResult.getLastEvaluatedKey)
                    .map(lastEvaluatedKey => queryRequest.clone().withExclusiveStartKey(lastEvaluatedKey))
                } yield Some((scanResult.getItems().asScala, queryForNextBatch))
              case None =>
                ZIO.none
            }
            .flatMap(resultList => ZStream.fromIterable(resultList))
        }.provide(dependencies)

        private def sendScanRequest[A](queryRequest: ScanRequest): ZIO[Any, DynamoDBZIOError, ScanResult] = {
          for {
            _ <- Logging.info(s"Starting scan: $queryRequest")
            results <-
              DynamoDBClient
                .scan(queryRequest)
                .mapError(ex => DynamoDBZIOError(s"Failed to execute scan $queryRequest : $ex"))
          } yield results
        }.provide(dependencies)

        def update[A, B](table: String, key: A, value: B)(implicit
            keySerializer: DynamoDBSerialiser[A],
            valueSerializer: DynamoDBUpdateSerialiser[B]
        ): IO[DynamoDBZIOError, Unit] =
          DynamoDBClient
            .updateItem(
              new UpdateItemRequest(
                table,
                keySerializer.serialise(key),
                valueSerializer.serialise(value)
              )
            )
            .bimap(
              ex => DynamoDBZIOError(s"Failed to write value '$value' to '$table': $ex"),
              _ => ()
            )
            .provide(dependencies)

        def create[A](table: String, keyName: String, value: A)(implicit
            valueSerializer: DynamoDBSerialiser[A]
        ): IO[DynamoDBZIOError, Unit] =
          DynamoDBClient
            .createItem(new PutItemRequest(table, valueSerializer.serialise(value)), keyName)
            .mapError(ex => DynamoDBZIOError(s"Failed to write value '$value' to '$table': $ex", Some(ex)))
            .unit
            .provide(dependencies)
      }
    }
}
