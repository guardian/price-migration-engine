package pricemigrationengine.services

import pricemigrationengine.model.ConfigurationFailure
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._
import zio._

object DynamoDBClientLive {
  val impl: ZLayer[Logging, ConfigurationFailure, DynamoDBClient] = {

    def acquireDynamoDb: ZIO[Logging, ConfigurationFailure, DynamoDbClient] =
      ZIO
        .attempt(AwsClient.dynamoDb)
        .mapError(ex => ConfigurationFailure(s"Failed to create the dynamoDb client: $ex"))

    def releaseDynamoDb(dynamoDb: DynamoDbClient): URIO[Logging, Unit] = {
      ZIO.unit
//      ZIO
//        .attempt(dynamoDb.close())
//        .catchAll(ex => Logging.error(s"Failed to close dynamo db connection: $ex"))
    }

    val dynamoDbLayer: ZLayer[Logging, ConfigurationFailure, DynamoDbClient] =
      ZLayer.fromAcquireRelease(acquireDynamoDb)(releaseDynamoDb)

    val serviceLayer: ZLayer[DynamoDbClient, Nothing, DynamoDBClient] = ZLayer.fromZIO {
      for {
        dynamoDb <- ZIO.service[DynamoDbClient]
      } yield new DynamoDBClient {
        def query(queryRequest: QueryRequest): Task[QueryResponse] = Task.attempt(dynamoDb.query(queryRequest))

        def scan(scanRequest: ScanRequest): Task[ScanResponse] = Task.attempt(dynamoDb.scan(scanRequest))

        def updateItem(updateRequest: UpdateItemRequest): Task[UpdateItemResponse] =
          Task.attempt(dynamoDb.updateItem(updateRequest))

        def createItem(createRequest: PutItemRequest, keyName: String): Task[PutItemResponse] =
          Task.attempt(
            dynamoDb.putItem(createRequest.copy(x => x.conditionExpression(s"attribute_not_exists($keyName)")))
          )

        def describeTable(tableName: String): Task[DescribeTableResponse] =
          Task.attempt(dynamoDb.describeTable(DescribeTableRequest.builder.tableName(tableName).build()))

        def createTable(request: CreateTableRequest): Task[CreateTableResponse] =
          Task.attempt(dynamoDb.createTable(request))

        def updateContinuousBackups(request: UpdateContinuousBackupsRequest): Task[UpdateContinuousBackupsResponse] =
          Task.attempt(dynamoDb.updateContinuousBackups(request))
      }
    }

    dynamoDbLayer >>> serviceLayer
  }
}
