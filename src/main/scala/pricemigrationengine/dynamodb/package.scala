package pricemigrationengine

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import zio.Has

package object dynamodb {
  type DynamoDBClient = Has[AmazonDynamoDBAsync]
  type DynamoDBZIO = Has[DynamoDBZIO.Service]
}
