package pricemigrationengine.services

import com.amazonaws.services.dynamodbv2.model.{QueryRequest, QueryResult, UpdateItemRequest, UpdateItemResult}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}
import zio.{ZIO, ZLayer, ZManaged}

object DynamoDBClient {
  val dynamoDB: ZLayer[Logging, String, DynamoDBClient] =
    ZLayer.fromManaged(
      ZManaged.fromFunctionM { logging: Logging =>
        ZManaged.make(
          ZIO
            .effect(
              AmazonDynamoDBClient
                .builder()
//                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-east-1"))
                .build()
            )
            .mapError(ex => s"Failed to create the dynamoDb client: $ex")
        ) { dynamoDB: AmazonDynamoDB =>
          ZIO
            .effect(dynamoDB.shutdown)
            .catchAll(ex => Logging.error(s"Failed to close dynamo db connection: $ex"))
        }.provide(logging)
      }
    )

  def query( queryRequest: QueryRequest): ZIO[DynamoDBClient, Throwable, QueryResult] = {
    ZIO.access(_.get.query(queryRequest))
  }

  def updateItem( updateRequest: UpdateItemRequest): ZIO[DynamoDBClient, Throwable, UpdateItemResult] = {
    ZIO.access(_.get.updateItem(updateRequest))
  }
}
