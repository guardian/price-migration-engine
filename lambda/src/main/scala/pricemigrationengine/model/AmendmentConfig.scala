package pricemigrationengine.model

import java.time.LocalDate

case class AmendmentConfig(
    // earliest date that price migration can take place
    earliestStartDate: LocalDate
)

case class ZuoraConfig(
    apiHost: String,
    clientId: String,
    clientSecret: String
)

case class DynamoDBConfig(endpoint: Option[DynamoDBEndpointConfig])

case class DynamoDBEndpointConfig(serviceEndpoint: String, signingRegion: String)

case class CohortTableConfig(batchSize: Int = 100)

case class StageConfig(stage: String)

case class SalesforceConfig(
    authUrl: String,
    clientId: String,
    clientSecret: String,
    userName: String,
    password: String,
    token: String
)

case class EmailSenderConfig(sqsEmailQueueName: String)

case class CohortStateMachineConfig(stateMachineArn: String)
