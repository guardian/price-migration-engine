package pricemigrationengine.model

sealed trait Failure {
  val reason: String
}

// General
case class InputFailure(reason: String) extends Failure
case class ConfigFailure(reason: String) extends Failure
case class DataExtractionFailure(reason: String) extends Failure

// Handlers
case class EstimationHandlerFailure(reason: String) extends Failure
case class NotificationHandlerFailure(reason: String) extends Failure
case class AmendmentFailure(reason: String) extends Failure
case class SalesforcePriceRiseWriteFailure(reason: String) extends Failure

// Cohorts
case class CohortStateMachineFailure(reason: String) extends Failure
case class CohortSpecFetchFailure(reason: String) extends Failure
case class CohortSpecUpdateFailure(reason: String) extends Failure
case class CohortTableCreateFailure(reason: String) extends Failure
case class CohortFetchFailure(reason: String) extends Failure
case class CohortCreateFailure(reason: String) extends Failure
case class CohortItemAlreadyPresentFailure(reason: String) extends Failure
case class CohortUpdateFailure(reason: String) extends Failure

// Zuora
case class ZuoraFetchFailure(reason: String) extends Failure
case class ZuoraUpdateFailure(reason: String) extends Failure
case class ZuoraAsynchronousOrderRequestFailure(reason: String) extends Failure
case class ZuoraGetJobStatusFailure(reason: String) extends Failure

// Salesforce
case class SalesforceFailure(reason: String) extends Failure

// S3
case class S3Failure(reason: String) extends Failure
case class SubscriptionIdUploadFailure(reason: String) extends Failure

// Braze
case class BrazeFailure(reason: String) extends Failure
