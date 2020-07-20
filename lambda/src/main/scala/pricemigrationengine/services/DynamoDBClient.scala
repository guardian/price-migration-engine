package pricemigrationengine.services

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}
import pricemigrationengine.model.ConfigurationFailure
import zio.{ZIO, ZLayer, ZManaged}

object DynamoDBClient {
  val dynamoDB: ZLayer[Logging with DynamoDBConfiguration, ConfigurationFailure, DynamoDBClient] =
    ZLayer.fromManaged(
      ZManaged.fromFunctionM { dependencies: Logging with DynamoDBConfiguration =>
        ZManaged
          .make(
            for {
              config <- DynamoDBConfiguration.dynamoDBConfig
              client <-
                ZIO
                  .effect(
                    config.endpoint
                      .foldLeft(AmazonDynamoDBClient.builder()) { (builder, endpoint) =>
                        builder.withEndpointConfiguration(
                          new AwsClientBuilder.EndpointConfiguration(endpoint.serviceEndpoint, endpoint.signingRegion)
                        )
                      }
                      .build()
                  )
                  .mapError(ex => ConfigurationFailure(s"Failed to create the dynamoDb client: $ex"))
            } yield client
          ) { dynamoDB: AmazonDynamoDB =>
            ZIO
              .effect(dynamoDB.shutdown())
              .catchAll(ex => Logging.error(s"Failed to close dynamo db connection: $ex"))
          }
          .provide(dependencies)
      }
    )

  def query(queryRequest: QueryRequest): ZIO[DynamoDBClient, Throwable, QueryResult] = {
    ZIO.accessM(client => ZIO.effect(client.get.query(queryRequest)))
  }

  def scan(scanRequest: ScanRequest): ZIO[DynamoDBClient, Throwable, ScanResult] = {
    ZIO.accessM(client => ZIO.effect(client.get.scan(scanRequest)))
  }

  def updateItem(updateRequest: UpdateItemRequest): ZIO[DynamoDBClient, Throwable, UpdateItemResult] = {
    ZIO.accessM(client => ZIO.effect(client.get.updateItem(updateRequest)))
  }

  def putItem(updateRequest: PutItemRequest): ZIO[DynamoDBClient, Throwable, PutItemResult] = {
    ZIO.accessM(client => ZIO.effect(client.get.putItem(updateRequest)))
  }

  def describeTable(tableName: String): ZIO[DynamoDBClient, Throwable, DescribeTableResult] =
    ZIO.accessM(client => ZIO.effect(client.get.describeTable(tableName)))

  def createTable(request: CreateTableRequest): ZIO[DynamoDBClient, Throwable, CreateTableResult] =
    ZIO.accessM(client => ZIO.effect(client.get.createTable(request)))

  def updateContinuousBackups(
      request: UpdateContinuousBackupsRequest
  ): ZIO[DynamoDBClient, Throwable, UpdateContinuousBackupsResult] =
    ZIO.accessM(client => ZIO.effect(client.get.updateContinuousBackups(request)))
}
