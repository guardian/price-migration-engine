package pricemigrationengine.services

import software.amazon.awssdk.services.dynamodb.model.{
  CreateTableRequest,
  CreateTableResponse,
  DescribeTableResponse,
  PutItemRequest,
  PutItemResponse,
  QueryRequest,
  QueryResponse,
  ScanRequest,
  ScanResponse,
  UpdateContinuousBackupsRequest,
  UpdateContinuousBackupsResponse,
  UpdateItemRequest,
  UpdateItemResponse
}
import zio.{RIO, Task, ZIO}

object DynamoDBClient {
  trait Service {
    def query(queryRequest: QueryRequest): Task[QueryResponse]
    def scan(scanRequest: ScanRequest): Task[ScanResponse]
    def updateItem(updateRequest: UpdateItemRequest): Task[UpdateItemResponse]
    def createItem(createRequest: PutItemRequest, keyName: String): Task[PutItemResponse]
    def describeTable(tableName: String): Task[DescribeTableResponse]
    def createTable(request: CreateTableRequest): Task[CreateTableResponse]
    def updateContinuousBackups(request: UpdateContinuousBackupsRequest): Task[UpdateContinuousBackupsResponse]
  }

  def query(queryRequest: QueryRequest): RIO[DynamoDBClient, QueryResponse] =
    ZIO.environmentWithZIO(_.get.query(queryRequest))

  def scan(scanRequest: ScanRequest): RIO[DynamoDBClient, ScanResponse] =
    ZIO.environmentWithZIO(_.get.scan(scanRequest))

  def updateItem(updateRequest: UpdateItemRequest): RIO[DynamoDBClient, UpdateItemResponse] =
    ZIO.environmentWithZIO(_.get.updateItem(updateRequest))

  def createItem(createRequest: PutItemRequest, keyName: String): RIO[DynamoDBClient, PutItemResponse] =
    ZIO.environmentWithZIO(_.get.createItem(createRequest, keyName))

  def describeTable(tableName: String): RIO[DynamoDBClient, DescribeTableResponse] =
    ZIO.environmentWithZIO(_.get.describeTable(tableName))

  def createTable(request: CreateTableRequest): RIO[DynamoDBClient, CreateTableResponse] =
    ZIO.environmentWithZIO(_.get.createTable(request))

  def updateContinuousBackups(
      request: UpdateContinuousBackupsRequest
  ): RIO[DynamoDBClient, UpdateContinuousBackupsResponse] =
    ZIO.environmentWithZIO(_.get.updateContinuousBackups(request))
}
