package pricemigrationengine.services

import pricemigrationengine.model.ConfigFailure
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._
import zio._

object DynamoDBClientLive {
  val impl: ZLayer[Logging, ConfigFailure, DynamoDBClient] = {

    def acquireDynamoDb: ZIO[Logging, ConfigFailure, DynamoDbClient] =
      ZIO
        .attempt(AwsClient.dynamoDb)
        .mapError(ex => ConfigFailure(s"Failed to create the dynamoDb client: $ex"))

    def releaseDynamoDb(dynamoDb: DynamoDbClient): URIO[Logging, Unit] = {
      /* In the interaction between DynamoDBZIOLive and DynamoDBClientLive the client is being released
       * while still in use, leading to 'java.lang.IllegalStateException: Connection pool shut down' exceptions.
       * Until this is sorted out, we'll avoid releasing the client, which is quite harmless.
       * See https://stackoverflow.com/questions/41209043/dynamodb-is-there-a-need-to-call-shutdown
       */
      ZIO.unit
    }

    val dynamoDbLayer: ZLayer[Logging, ConfigFailure, DynamoDbClient] =
      ZLayer.scoped(ZIO.acquireRelease(acquireDynamoDb)(releaseDynamoDb))

    val serviceLayer: ZLayer[DynamoDbClient, Nothing, DynamoDBClient] = ZLayer.fromZIO {
      for {
        dynamoDb <- ZIO.service[DynamoDbClient]
      } yield new DynamoDBClient {
        def query(queryRequest: QueryRequest): Task[QueryResponse] = ZIO.attempt(dynamoDb.query(queryRequest))

        def scan(scanRequest: ScanRequest): Task[ScanResponse] = ZIO.attempt(dynamoDb.scan(scanRequest))

        def updateItem(updateRequest: UpdateItemRequest): Task[UpdateItemResponse] =
          ZIO.attempt(dynamoDb.updateItem(updateRequest))

        def createItem(createRequest: PutItemRequest, keyName: String): Task[PutItemResponse] =
          ZIO.attempt(
            dynamoDb.putItem(createRequest.copy(x => x.conditionExpression(s"attribute_not_exists($keyName)")))
          )

        def describeTable(tableName: String): Task[DescribeTableResponse] =
          ZIO.attempt(dynamoDb.describeTable(DescribeTableRequest.builder.tableName(tableName).build()))

        def createTable(request: CreateTableRequest): Task[CreateTableResponse] =
          ZIO.attempt(dynamoDb.createTable(request))

        def updateContinuousBackups(request: UpdateContinuousBackupsRequest): Task[UpdateContinuousBackupsResponse] =
          ZIO.attempt(dynamoDb.updateContinuousBackups(request))
      }
    }

    dynamoDbLayer >>> serviceLayer
  }
}
