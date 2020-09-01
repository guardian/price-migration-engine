package pricemigrationengine.services

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}
import pricemigrationengine.model.ConfigurationFailure
import zio._

object DynamoDBClientLive {
  val impl: ZLayer[DynamoDBConfiguration with Logging, ConfigurationFailure, DynamoDBClient] = {

    def acquireDynamoDb: ZIO[DynamoDBConfiguration with Logging, ConfigurationFailure, AmazonDynamoDB] =
      for {
        config <- DynamoDBConfiguration.dynamoDBConfig
        client <-
          ZIO
            .effect(
              config.endpoint
                .foldLeft(AmazonDynamoDBClient.builder())((builder, endpoint) =>
                  builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(endpoint.serviceEndpoint, endpoint.signingRegion)
                  )
                )
                .build()
            )
            .mapError(ex => ConfigurationFailure(s"Failed to create the dynamoDb client: $ex"))
      } yield client

    def releaseDynamoDb(dynamoDb: AmazonDynamoDB): URIO[Logging, Unit] =
      ZIO
        .effect(dynamoDb.shutdown())
        .catchAll(ex => Logging.error(s"Failed to close dynamo db connection: $ex"))

    val dynamoDbLayer: ZLayer[DynamoDBConfiguration with Logging, ConfigurationFailure, Has[AmazonDynamoDB]] =
      ZLayer.fromAcquireRelease(acquireDynamoDb)(releaseDynamoDb)

    val serviceLayer: ZLayer[Has[AmazonDynamoDB], Nothing, DynamoDBClient] = ZLayer.fromService { dynamoDb =>
      new DynamoDBClient.Service {
        def query(queryRequest: QueryRequest): Task[QueryResult] = Task.effect(dynamoDb.query(queryRequest))

        def scan(scanRequest: ScanRequest): Task[ScanResult] = Task.effect(dynamoDb.scan(scanRequest))

        def updateItem(updateRequest: UpdateItemRequest): Task[UpdateItemResult] =
          Task.effect(dynamoDb.updateItem(updateRequest))

        def createItem(createRequest: PutItemRequest, keyName: String): Task[PutItemResult] =
          Task.effect(dynamoDb.putItem(createRequest.withConditionExpression(s"attribute_not_exists($keyName)")))

        def describeTable(tableName: String): Task[DescribeTableResult] = Task.effect(dynamoDb.describeTable(tableName))

        def createTable(request: CreateTableRequest): Task[CreateTableResult] =
          Task.effect(dynamoDb.createTable(request))

        def updateContinuousBackups(request: UpdateContinuousBackupsRequest): Task[UpdateContinuousBackupsResult] =
          Task.effect(dynamoDb.updateContinuousBackups(request))
      }
    }

    dynamoDbLayer >>> serviceLayer
  }
}
