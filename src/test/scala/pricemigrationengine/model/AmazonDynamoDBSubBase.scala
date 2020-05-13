package pricemigrationengine.model

import java.{lang, util}

import com.amazonaws.{AmazonWebServiceRequest, ResponseMetadata}
import com.amazonaws.regions.Region
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{AttributeDefinition, AttributeValue, AttributeValueUpdate, BatchGetItemRequest, BatchGetItemResult, BatchWriteItemRequest, BatchWriteItemResult, Condition, CreateBackupRequest, CreateBackupResult, CreateGlobalTableRequest, CreateGlobalTableResult, CreateTableRequest, CreateTableResult, DeleteBackupRequest, DeleteBackupResult, DeleteItemRequest, DeleteItemResult, DeleteTableRequest, DeleteTableResult, DescribeBackupRequest, DescribeBackupResult, DescribeContinuousBackupsRequest, DescribeContinuousBackupsResult, DescribeContributorInsightsRequest, DescribeContributorInsightsResult, DescribeEndpointsRequest, DescribeEndpointsResult, DescribeGlobalTableRequest, DescribeGlobalTableResult, DescribeGlobalTableSettingsRequest, DescribeGlobalTableSettingsResult, DescribeLimitsRequest, DescribeLimitsResult, DescribeTableReplicaAutoScalingRequest, DescribeTableReplicaAutoScalingResult, DescribeTableRequest, DescribeTableResult, DescribeTimeToLiveRequest, DescribeTimeToLiveResult, GetItemRequest, GetItemResult, KeySchemaElement, KeysAndAttributes, ListBackupsRequest, ListBackupsResult, ListContributorInsightsRequest, ListContributorInsightsResult, ListGlobalTablesRequest, ListGlobalTablesResult, ListTablesRequest, ListTablesResult, ListTagsOfResourceRequest, ListTagsOfResourceResult, ProvisionedThroughput, PutItemRequest, PutItemResult, QueryRequest, QueryResult, RestoreTableFromBackupRequest, RestoreTableFromBackupResult, RestoreTableToPointInTimeRequest, RestoreTableToPointInTimeResult, ScanRequest, ScanResult, TagResourceRequest, TagResourceResult, TransactGetItemsRequest, TransactGetItemsResult, TransactWriteItemsRequest, TransactWriteItemsResult, UntagResourceRequest, UntagResourceResult, UpdateContinuousBackupsRequest, UpdateContinuousBackupsResult, UpdateContributorInsightsRequest, UpdateContributorInsightsResult, UpdateGlobalTableRequest, UpdateGlobalTableResult, UpdateGlobalTableSettingsRequest, UpdateGlobalTableSettingsResult, UpdateItemRequest, UpdateItemResult, UpdateTableReplicaAutoScalingRequest, UpdateTableReplicaAutoScalingResult, UpdateTableRequest, UpdateTableResult, UpdateTimeToLiveRequest, UpdateTimeToLiveResult, WriteRequest}
import com.amazonaws.services.dynamodbv2.waiters.AmazonDynamoDBWaiters

/**
 * This trait provides default 'implementations' for for all AmazonDynamoDB to avoid clutter when creating stubs of
 * this interface.
 */
trait AmazonDynamoDBSubBase extends AmazonDynamoDB {
  override def setEndpoint(endpoint: ZuoraProductRatePlanChargeId): Unit = ???

  override def setRegion(region: Region): Unit = ???

  override def batchGetItem(batchGetItemRequest: BatchGetItemRequest): BatchGetItemResult = ???

  override def batchGetItem(requestItems: util.Map[ZuoraProductRatePlanChargeId, KeysAndAttributes], returnConsumedCapacity: ZuoraProductRatePlanChargeId): BatchGetItemResult = ???

  override def batchGetItem(requestItems: util.Map[ZuoraProductRatePlanChargeId, KeysAndAttributes]): BatchGetItemResult = ???

  override def batchWriteItem(batchWriteItemRequest: BatchWriteItemRequest): BatchWriteItemResult = ???

  override def batchWriteItem(requestItems: util.Map[ZuoraProductRatePlanChargeId, util.List[WriteRequest]]): BatchWriteItemResult = ???

  override def createBackup(createBackupRequest: CreateBackupRequest): CreateBackupResult = ???

  override def createGlobalTable(createGlobalTableRequest: CreateGlobalTableRequest): CreateGlobalTableResult = ???

  override def createTable(createTableRequest: CreateTableRequest): CreateTableResult = ???

  override def createTable(attributeDefinitions: util.List[AttributeDefinition], tableName: ZuoraProductRatePlanChargeId, keySchema: util.List[KeySchemaElement], provisionedThroughput: ProvisionedThroughput): CreateTableResult = ???

  override def deleteBackup(deleteBackupRequest: DeleteBackupRequest): DeleteBackupResult = ???

  override def deleteItem(deleteItemRequest: DeleteItemRequest): DeleteItemResult = ???

  override def deleteItem(tableName: ZuoraProductRatePlanChargeId, key: util.Map[ZuoraProductRatePlanChargeId, AttributeValue]): DeleteItemResult = ???

  override def deleteItem(tableName: ZuoraProductRatePlanChargeId, key: util.Map[ZuoraProductRatePlanChargeId, AttributeValue], returnValues: ZuoraProductRatePlanChargeId): DeleteItemResult = ???

  override def deleteTable(deleteTableRequest: DeleteTableRequest): DeleteTableResult = ???

  override def deleteTable(tableName: ZuoraProductRatePlanChargeId): DeleteTableResult = ???

  override def describeBackup(describeBackupRequest: DescribeBackupRequest): DescribeBackupResult = ???

  override def describeContinuousBackups(describeContinuousBackupsRequest: DescribeContinuousBackupsRequest): DescribeContinuousBackupsResult = ???

  override def describeContributorInsights(describeContributorInsightsRequest: DescribeContributorInsightsRequest): DescribeContributorInsightsResult = ???

  override def describeEndpoints(describeEndpointsRequest: DescribeEndpointsRequest): DescribeEndpointsResult = ???

  override def describeGlobalTable(describeGlobalTableRequest: DescribeGlobalTableRequest): DescribeGlobalTableResult = ???

  override def describeGlobalTableSettings(describeGlobalTableSettingsRequest: DescribeGlobalTableSettingsRequest): DescribeGlobalTableSettingsResult = ???

  override def describeLimits(describeLimitsRequest: DescribeLimitsRequest): DescribeLimitsResult = ???

  override def describeTable(describeTableRequest: DescribeTableRequest): DescribeTableResult = ???

  override def describeTable(tableName: ZuoraProductRatePlanChargeId): DescribeTableResult = ???

  override def describeTableReplicaAutoScaling(describeTableReplicaAutoScalingRequest: DescribeTableReplicaAutoScalingRequest): DescribeTableReplicaAutoScalingResult = ???

  override def describeTimeToLive(describeTimeToLiveRequest: DescribeTimeToLiveRequest): DescribeTimeToLiveResult = ???

  override def getItem(getItemRequest: GetItemRequest): GetItemResult = ???

  override def getItem(tableName: ZuoraProductRatePlanChargeId, key: util.Map[ZuoraProductRatePlanChargeId, AttributeValue]): GetItemResult = ???

  override def getItem(tableName: ZuoraProductRatePlanChargeId, key: util.Map[ZuoraProductRatePlanChargeId, AttributeValue], consistentRead: lang.Boolean): GetItemResult = ???

  override def listBackups(listBackupsRequest: ListBackupsRequest): ListBackupsResult = ???

  override def listContributorInsights(listContributorInsightsRequest: ListContributorInsightsRequest): ListContributorInsightsResult = ???

  override def listGlobalTables(listGlobalTablesRequest: ListGlobalTablesRequest): ListGlobalTablesResult = ???

  override def listTables(listTablesRequest: ListTablesRequest): ListTablesResult = ???

  override def listTables(): ListTablesResult = ???

  override def listTables(exclusiveStartTableName: ZuoraProductRatePlanChargeId): ListTablesResult = ???

  override def listTables(exclusiveStartTableName: ZuoraProductRatePlanChargeId, limit: Integer): ListTablesResult = ???

  override def listTables(limit: Integer): ListTablesResult = ???

  override def listTagsOfResource(listTagsOfResourceRequest: ListTagsOfResourceRequest): ListTagsOfResourceResult = ???

  override def putItem(putItemRequest: PutItemRequest): PutItemResult = ???

  override def putItem(tableName: ZuoraProductRatePlanChargeId, item: util.Map[ZuoraProductRatePlanChargeId, AttributeValue]): PutItemResult = ???

  override def putItem(tableName: ZuoraProductRatePlanChargeId, item: util.Map[ZuoraProductRatePlanChargeId, AttributeValue], returnValues: ZuoraProductRatePlanChargeId): PutItemResult = ???

  override def query(queryRequest: QueryRequest): QueryResult = ???

  override def restoreTableFromBackup(restoreTableFromBackupRequest: RestoreTableFromBackupRequest): RestoreTableFromBackupResult = ???

  override def restoreTableToPointInTime(restoreTableToPointInTimeRequest: RestoreTableToPointInTimeRequest): RestoreTableToPointInTimeResult = ???

  override def scan(scanRequest: ScanRequest): ScanResult = ???

  override def scan(tableName: ZuoraProductRatePlanChargeId, attributesToGet: util.List[ZuoraProductRatePlanChargeId]): ScanResult = ???

  override def scan(tableName: ZuoraProductRatePlanChargeId, scanFilter: util.Map[ZuoraProductRatePlanChargeId, Condition]): ScanResult = ???

  override def scan(tableName: ZuoraProductRatePlanChargeId, attributesToGet: util.List[ZuoraProductRatePlanChargeId], scanFilter: util.Map[ZuoraProductRatePlanChargeId, Condition]): ScanResult = ???

  override def tagResource(tagResourceRequest: TagResourceRequest): TagResourceResult = ???

  override def transactGetItems(transactGetItemsRequest: TransactGetItemsRequest): TransactGetItemsResult = ???

  override def transactWriteItems(transactWriteItemsRequest: TransactWriteItemsRequest): TransactWriteItemsResult = ???

  override def untagResource(untagResourceRequest: UntagResourceRequest): UntagResourceResult = ???

  override def updateContinuousBackups(updateContinuousBackupsRequest: UpdateContinuousBackupsRequest): UpdateContinuousBackupsResult = ???

  override def updateContributorInsights(updateContributorInsightsRequest: UpdateContributorInsightsRequest): UpdateContributorInsightsResult = ???

  override def updateGlobalTable(updateGlobalTableRequest: UpdateGlobalTableRequest): UpdateGlobalTableResult = ???

  override def updateGlobalTableSettings(updateGlobalTableSettingsRequest: UpdateGlobalTableSettingsRequest): UpdateGlobalTableSettingsResult = ???

  override def updateItem(updateItemRequest: UpdateItemRequest): UpdateItemResult = ???

  override def updateItem(tableName: ZuoraProductRatePlanChargeId, key: util.Map[ZuoraProductRatePlanChargeId, AttributeValue], attributeUpdates: util.Map[ZuoraProductRatePlanChargeId, AttributeValueUpdate]): UpdateItemResult = ???

  override def updateItem(tableName: ZuoraProductRatePlanChargeId, key: util.Map[ZuoraProductRatePlanChargeId, AttributeValue], attributeUpdates: util.Map[ZuoraProductRatePlanChargeId, AttributeValueUpdate], returnValues: ZuoraProductRatePlanChargeId): UpdateItemResult = ???

  override def updateTable(updateTableRequest: UpdateTableRequest): UpdateTableResult = ???

  override def updateTable(tableName: ZuoraProductRatePlanChargeId, provisionedThroughput: ProvisionedThroughput): UpdateTableResult = ???

  override def updateTableReplicaAutoScaling(updateTableReplicaAutoScalingRequest: UpdateTableReplicaAutoScalingRequest): UpdateTableReplicaAutoScalingResult = ???

  override def updateTimeToLive(updateTimeToLiveRequest: UpdateTimeToLiveRequest): UpdateTimeToLiveResult = ???

  override def shutdown(): Unit = ???

  override def getCachedResponseMetadata(request: AmazonWebServiceRequest): ResponseMetadata = ???

  override def waiters(): AmazonDynamoDBWaiters = ???
}
