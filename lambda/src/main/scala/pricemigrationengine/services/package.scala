package pricemigrationengine


package object services {

  type ZuoraConfiguration = ZuoraConfiguration.Service
  type CohortTableConfiguration = CohortTableConfiguration.Service
  type SalesforceConfiguration = SalesforceConfiguration.Service
  type StageConfiguration = StageConfiguration.Service
  type EmailSenderConfiguration = EmailSenderConfiguration.Service
  type CohortStateMachineConfiguration = CohortStateMachineConfiguration.Service
  type ExportConfiguration = ExportConfiguration.Service

  type CohortStateMachine = CohortStateMachine.Service
  type CohortSpecTable = CohortSpecTable.Service
  type CohortTable = CohortTable.Service
  type CohortTableDdl = CohortTableDdl.Service
  type Zuora = Zuora.Service
  type Logging = Logging.Service
  type DynamoDBClient = DynamoDBClient.Service
  type DynamoDBZIO = DynamoDBZIO.Service
  type SalesforceClient = SalesforceClient.Service
  type S3 = S3.Service
  type EmailSender = EmailSender.Service
}
