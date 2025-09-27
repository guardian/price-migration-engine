package pricemigrationengine.model

sealed trait CohortTableFilter { val value: String }

// When adding a state, remember to update 'all'.
object CohortTableFilter {

  // ++++++++++ Normal states ++++++++++

  case object ReadyForEstimation extends CohortTableFilter { override val value: String = "ReadyForEstimation" }

  case object EstimationComplete extends CohortTableFilter { override val value: String = "EstimationComplete" }

  case object SalesforcePriceRiseCreationComplete extends CohortTableFilter {
    override val value: String = "SalesforcePriceRiseCreationComplete"
  }

  case object NotificationSendComplete extends CohortTableFilter {
    override val value: String = "NotificationSendComplete"
  }

  case object NotificationSendDateWrittenToSalesforce extends CohortTableFilter {
    override val value: String = "NotificationSendDateWrittenToSalesforce"
  }

  case object AmendmentComplete extends CohortTableFilter { override val value: String = "AmendmentComplete" }

  case object AmendmentWrittenToSalesforce extends CohortTableFilter {
    override val value: String = "AmendmentWrittenToSalesforce"
  }

  case object DoNotProcessUntil extends CohortTableFilter { override val value: String = "DoNotProcessUntil" }

  // ++++++++++ Exceptional states ++++++++++

  case object ZuoraCancellation extends CohortTableFilter { override val value: String = "ZuoraCancellation" }
  case object ZuoraEmptyInvoicePreview extends CohortTableFilter {
    override val value: String = "ZuoraEmptyInvoicePreview"
  }

  // General termination processing state. This is a terminal state for a cohort item
  // It is used when the processing of a cohort items cannot pursue (for another reason than)
  // the subscription having been cancelled in Zuora
  case object Cancelled extends CohortTableFilter { override val value: String = "Cancelled" }

  /*
   * Status of a sub where the estimation indicates that its price will not increase,
   * so is ineligible for further processing.
   */
  case object NoPriceIncrease extends CohortTableFilter { override val value: String = "NoPriceIncrease" }

  // +++++++++++++++++++

  // Remember to update when adding a state that is being used to query cohort items
  // In alphabetical order
  val allQueryableStates: Set[CohortTableFilter] = Set(
    AmendmentComplete,
    AmendmentWrittenToSalesforce,
    Cancelled,
    DoNotProcessUntil,
    EstimationComplete,
    NoPriceIncrease,
    NotificationSendComplete,
    NotificationSendDateWrittenToSalesforce,
    ReadyForEstimation,
    SalesforcePriceRiseCreationComplete,
  )
}
