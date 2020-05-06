package pricemigrationengine

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import zio.Has

package object dynamodb {
  type DynamoDBClient = Has[AmazonDynamoDB]
  type DynamoDBZIO = Has[DynamoDBZIO.Service]
}
