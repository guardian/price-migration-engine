package pricemigrationengine.model

sealed trait Failure {
  val reason: String
}

case class InputFailure(reason: String) extends Failure
case class ConfigFailure(reason: String) extends Failure

case class CohortStateMachineFailure(reason: String) extends Failure

case class CohortSpecFetchFailure(reason: String) extends Failure
case class CohortSpecUpdateFailure(reason: String) extends Failure

case class CohortTableCreateFailure(reason: String) extends Failure

case class CohortFetchFailure(reason: String) extends Failure
case class CohortCreateFailure(reason: String) extends Failure
case class CohortItemAlreadyPresentFailure(reason: String) extends Failure
case class CohortUpdateFailure(reason: String) extends Failure

case class ZuoraFailure(reason: String) extends Failure
case class ZuoraFetchFailure(reason: String) extends Failure
case class ZuoraUpdateFailure(reason: String) extends Failure

case class AmendmentDataFailure(reason: String) extends Failure
case class CancelledSubscriptionFailure(reason: String) extends Failure
case class ExpiringSubscriptionFailure(reason: String) extends Failure

case class SalesforcePriceRiseWriteFailure(reason: String) extends Failure
case class SalesforceClientFailure(reason: String) extends Failure

case class S3Failure(reason: String) extends Failure
case class SubscriptionIdUploadFailure(reason: String) extends Failure

case class NotificationHandlerFailure(reason: String) extends Failure
case class NotificationNotEnoughLeadTimeFailure(reason: String) extends Failure
case class EmailSenderFailure(reason: String) extends Failure

case class CohortTableDatalakeExportFailure(reason: String) extends Failure
