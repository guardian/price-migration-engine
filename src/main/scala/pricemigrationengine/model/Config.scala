package pricemigrationengine.model

import java.time.LocalDate

case class Config(
    zuora: ZuoraConfig,
    dynamoDBConfig: DynamoDBConfig,
    stage: String,
    yearInFuture: LocalDate = LocalDate.now.plusYears(1)
)

case class ZuoraConfig(apiHost: String, clientId: String, clientSecret: String)

case class DynamoDBConfig(endpoint: Option[DynamoDBEndpointConfig])

case class DynamoDBEndpointConfig(serviceEndpoint: String, signingRegion: String)
