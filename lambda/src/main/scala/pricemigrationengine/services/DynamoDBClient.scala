package pricemigrationengine.services

import com.amazonaws.services.dynamodbv2.model._
import zio.{RIO, Task}

object DynamoDBClient {
  trait Service {
    def query(queryRequest: QueryRequest): Task[QueryResult]
    def scan(scanRequest: ScanRequest): Task[ScanResult]
    def updateItem(updateRequest: UpdateItemRequest): Task[UpdateItemResult]
    def createItem(createRequest: PutItemRequest, keyName: String): Task[PutItemResult]
    def describeTable(tableName: String): Task[DescribeTableResult]
    def createTable(request: CreateTableRequest): Task[CreateTableResult]
    def updateContinuousBackups(request: UpdateContinuousBackupsRequest): Task[UpdateContinuousBackupsResult]
  }

  def query(queryRequest: QueryRequest): RIO[DynamoDBClient, QueryResult] = RIO.accessM(_.get.query(queryRequest))

  def scan(scanRequest: ScanRequest): RIO[DynamoDBClient, ScanResult] = RIO.accessM(_.get.scan(scanRequest))

  def updateItem(updateRequest: UpdateItemRequest): RIO[DynamoDBClient, UpdateItemResult] =
    RIO.accessM(_.get.updateItem(updateRequest))

  def createItem(createRequest: PutItemRequest, keyName: String): RIO[DynamoDBClient, PutItemResult] =
    RIO.accessM(_.get.createItem(createRequest, keyName))

  def describeTable(tableName: String): RIO[DynamoDBClient, DescribeTableResult] =
    RIO.accessM(_.get.describeTable(tableName))

  def createTable(request: CreateTableRequest): RIO[DynamoDBClient, CreateTableResult] =
    RIO.accessM(_.get.createTable(request))

  def updateContinuousBackups(
      request: UpdateContinuousBackupsRequest
  ): RIO[DynamoDBClient, UpdateContinuousBackupsResult] = RIO.accessM(_.get.updateContinuousBackups(request))
}
