package pricemigrationengine.model

case class Config(stage: String, zuora: ZuoraConfig, dynamoDBConfig: DynamoDBConfig)

case class ZuoraConfig(baseUrl: String, clientId: String, clientSecret: String)

case class DynamoDBConfig(endpoint: Option[DynamoDBEndpointConfig])

case class DynamoDBEndpointConfig(serviceEndpoint: String, signingRegion: String)