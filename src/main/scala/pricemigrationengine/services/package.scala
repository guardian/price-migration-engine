package pricemigrationengine

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import zio.Has

package object services {
  type Configuration = Has[Configuration.Service]
  type CohortTable = Has[CohortTable.Service]
  type Zuora = Has[Zuora.Service]
  type Logging = Has[Logging.Service]
  type DynamoDBClient = Has[AmazonDynamoDB]
  type DynamoDBZIO = Has[DynamoDBZIO.Service]
}
