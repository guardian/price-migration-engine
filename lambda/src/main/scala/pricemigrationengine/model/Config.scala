package pricemigrationengine.model

import java.time.LocalDate

case class Config(
    dynamoDBConfig: DynamoDBConfig,
    stage: String,
    // earliest date that price migration can take place
    earliestStartDate: LocalDate,
    // max number of subscriptions to process in one go
    batchSize: Int = 100
)

case class ZuoraConfig(apiHost: String, clientId: String, clientSecret: String, yearInFuture: LocalDate = LocalDate.now.plusYears(1))

case class DynamoDBConfig(endpoint: Option[DynamoDBEndpointConfig])

case class DynamoDBEndpointConfig(serviceEndpoint: String, signingRegion: String)
