package pricemigrationengine.model

case class ZuoraConfig(
    apiHost: String,
    clientId: String,
    clientSecret: String
)

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

case class ExportConfig(
    exportBucketName: String
)
