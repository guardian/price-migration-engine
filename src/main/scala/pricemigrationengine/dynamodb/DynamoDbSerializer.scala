package pricemigrationengine.dynamodb

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest}
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

object DynamoDbSerializer {
  trait Service {
    def query[A](query: QueryRequest, deserializer: java.util.Map[String, AttributeValue] => IO[String, A]): ZStream[Any, String, A]
  }

  val impl:  ZLayer[DynamoDBClient, String, DynamoDbSerializer] =
    ZLayer.fromFunction { dynamoDbClient: DynamoDBClient =>
      new Service {
        override def query[A](query: QueryRequest, deserializer: java.util.Map[String, AttributeValue] => IO[String, A]): ZStream[Any, String, A] = {
//          val queryRequest = new QueryRequest()
//            .withTableName("PriceMigrationEngineDEV")
//            .withKeyConditionExpression("ProcessingStageIndex ")
//            .withKeyConditionExpression("ProcessingStageIndex = :processingStage")
//            .withExpressionAttributeValues(
//              Map("processingStage" -> new AttributeValue("Processing stage")).asJava
//            )

          ZStream
            .unfoldM(query) { queryRequest =>
              if(queryRequest.getExclusiveStartKey() != null && queryRequest.getExclusiveStartKey.isEmpty()) {
                ZIO.succeed(None)
              } else {
                ZIO
                  .effect(dynamoDbClient.get.query(queryRequest))
                  .mapError(ex => s"Failed to execute query $queryRequest : $ex")
                  .map(queryResult =>
                    Some(
                      (
                        queryResult.getItems().asScala,
                        queryRequest.withExclusiveStartKey(queryResult.getLastEvaluatedKey())
                      )
                    )
                  )
              }
            }
            .flatMap(resultList =>  ZStream.fromIterable(resultList))
            .mapM(deserializer)
        }
      }
    }
}
