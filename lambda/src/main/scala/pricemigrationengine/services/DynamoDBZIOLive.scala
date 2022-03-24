package pricemigrationengine.services

import software.amazon.awssdk.services.dynamodb.model._
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer}

import java.util
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

        private def recursivelyExecuteQueryUntilAllResultsAreStreamed(
            query: QueryRequest
        ): ZStream[Any, DynamoDBZIOError, util.Map[String, AttributeValue]] = {
          ZStream
            .unfoldM(Some(query).asInstanceOf[Option[QueryRequest]]) {
              case Some(queryRequest) =>
                for {
                  queryResult <- sendQueryRequest(queryRequest)
                  _ <- Logging.info(s"Received query results for batch with ${queryResult.items.asScala.length} items")
                  queryForNextBatch = Option(queryResult.lastEvaluatedKey)
                    .filterNot(_.isEmpty)
                    .map(lastEvaluatedKey => queryRequest.copy(x => x.exclusiveStartKey(lastEvaluatedKey)))
                } yield Some((queryResult.items.asScala, queryForNextBatch))
              case None =>
                ZIO.none
            }
            .flatMap(resultList => ZStream.fromIterable(resultList))
        }.provide(dependencies)

        private def sendQueryRequest(queryRequest: QueryRequest): ZIO[Any, DynamoDBZIOError, QueryResponse] = {
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

        private def recursivelyExecuteScanUntilAllResultsAreStreamed(
            query: ScanRequest
        ): ZStream[Any, DynamoDBZIOError, util.Map[String, AttributeValue]] = {
          ZStream
            .unfoldM(Some(query).asInstanceOf[Option[ScanRequest]]) {
              case Some(queryRequest) =>
                for {
                  scanResult <- sendScanRequest(queryRequest)
                  _ <- Logging.info(s"Received query results for batch with ${scanResult.count} items")
                  queryForNextBatch = Option(scanResult.lastEvaluatedKey)
                    .filterNot(_.isEmpty)
                    .map(lastEvaluatedKey => queryRequest.copy(x => x.exclusiveStartKey(lastEvaluatedKey)))
                } yield Some((scanResult.items.asScala, queryForNextBatch))
              case None =>
                ZIO.none
            }
            .flatMap(resultList => ZStream.fromIterable(resultList))
        }.provide(dependencies)

        private def sendScanRequest(queryRequest: ScanRequest): ZIO[Any, DynamoDBZIOError, ScanResponse] = {
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
              UpdateItemRequest.builder
                .tableName(table)
                .key(keySerializer.serialise(key))
                .attributeUpdates(valueSerializer.serialise(value))
                .build()
            )
            .mapBoth(
              ex => DynamoDBZIOError(s"Failed to write value '$value' to '$table': $ex"),
              _ => ()
            )
            .provide(dependencies)

        def create[A](table: String, keyName: String, value: A)(implicit
            valueSerializer: DynamoDBSerialiser[A]
        ): IO[DynamoDBZIOError, Unit] =
          DynamoDBClient
            .createItem(PutItemRequest.builder.tableName(table).item(valueSerializer.serialise(value)).build(), keyName)
            .mapError(ex => DynamoDBZIOError(s"Failed to write value '$value' to '$table': $ex", Some(ex)))
            .unit
            .provide(dependencies)
      }
    }
}
