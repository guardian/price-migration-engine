package pricemigrationengine.dynamodb

import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClient}
import zio.console.Console
import zio.{ZIO, ZLayer, ZManaged}

import scala.util.Try

object DynamoDBClient {
  val dynamoDB: ZLayer[Console, String, DynamoDBClient] =
    ZLayer.fromManaged(
      ZManaged.fromFunctionM { console: Console =>
        ZManaged.make(
          ZIO
            .effect(AmazonDynamoDBAsyncClient.asyncBuilder().build())
            .mapError(ex => s"Failed to create the dynamoDb client: $ex")
        ) { dynamoDB: AmazonDynamoDBAsync =>
          ZIO.effectTotal(
            Try(dynamoDB.shutdown)
              .recover {
                case ex => console.get.putStrLn(s"Failed to close dynamo db connection: $ex")
              }
          )
        }
      }
    )
}