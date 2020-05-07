package pricemigrationengine.dynamodb

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}
import zio.console.Console
import zio.{ZIO, ZLayer, ZManaged}

object DynamoDBClient {
  val dynamoDB: ZLayer[Console, String, DynamoDBClient] =
    ZLayer.fromManaged(
      ZManaged.fromFunctionM { console: Console =>
        ZManaged.make(
          ZIO
            .effect(
              AmazonDynamoDBClient
                .builder()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-east-1"))
                .build()
            )
            .mapError(ex => s"Failed to create the dynamoDb client: $ex")
        ) { dynamoDB: AmazonDynamoDB =>
          ZIO
            .effect(dynamoDB.shutdown)
            .catchAll(ex => console.get.putStrLn(s"Failed to close dynamo db connection: $ex"))
        }
      }
    )
}