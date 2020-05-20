package pricemigrationengine

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import zio.Has

package object services {
  type EstimationHandlerConfiguration = Has[EstimationHandlerConfiguration.Service]
  type ZuoraConfiguration = Has[ZuoraConfiguration.Service]
  type DynamoDBConfiguration = Has[DynamoDBConfiguration.Service]
  type CohortTableConfiguration = Has[CohortTableConfiguration.Service]
  type SalesforceConfiguration = Has[SalesforceConfiguration.Service]
  type CohortTable = Has[CohortTable.Service]
  type Zuora = Has[Zuora.Service]
  type Logging = Has[Logging.Service]
  type DynamoDBClient = Has[AmazonDynamoDB]
  type DynamoDBZIO = Has[DynamoDBZIO.Service]
  type SalesforceClient = Has[SalesforceClient.Service]
}
