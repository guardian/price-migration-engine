package pricemigrationengine.services

import pricemigrationengine.model.ConfigurationFailure
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._
import zio._

object DynamoDBClientLive {
  val impl: ZLayer[Logging, ConfigurationFailure, DynamoDBClient] = {

    def acquireDynamoDb: ZIO[Logging, ConfigurationFailure, DynamoDbClient] =
      ZIO
        .effect(DynamoDbClient.create())
        .mapError(ex => ConfigurationFailure(s"Failed to create the dynamoDb client: $ex"))

    def releaseDynamoDb(dynamoDb: DynamoDbClient): URIO[Logging, Unit] =
      ZIO
        .effect(dynamoDb.close())
        .catchAll(ex => Logging.error(s"Failed to close dynamo db connection: $ex"))

    val dynamoDbLayer: ZLayer[Logging, ConfigurationFailure, Has[DynamoDbClient]] =
      ZLayer.fromAcquireRelease(acquireDynamoDb)(releaseDynamoDb)

    val serviceLayer: ZLayer[Has[DynamoDbClient], Nothing, DynamoDBClient] = ZLayer.fromService { dynamoDb =>
      new DynamoDBClient.Service {
        def query(queryRequest: QueryRequest): Task[QueryResponse] = Task.effect(dynamoDb.query(queryRequest))

        def scan(scanRequest: ScanRequest): Task[ScanResponse] = Task.effect(dynamoDb.scan(scanRequest))

        def updateItem(updateRequest: UpdateItemRequest): Task[UpdateItemResponse] =
          Task.effect(dynamoDb.updateItem(updateRequest))

        def createItem(createRequest: PutItemRequest, keyName: String): Task[PutItemResponse] =
          Task.effect(
            dynamoDb.putItem(createRequest.copy(x => x.conditionExpression(s"attribute_not_exists($keyName)")))
          )

        def describeTable(tableName: String): Task[DescribeTableResponse] =
          Task.effect(dynamoDb.describeTable(DescribeTableRequest.builder.tableName(tableName).build()))

        def createTable(request: CreateTableRequest): Task[CreateTableResponse] =
          Task.effect(dynamoDb.createTable(request))

        def updateContinuousBackups(request: UpdateContinuousBackupsRequest): Task[UpdateContinuousBackupsResponse] =
          Task.effect(dynamoDb.updateContinuousBackups(request))
      }
    }

    dynamoDbLayer >>> serviceLayer
  }
}
