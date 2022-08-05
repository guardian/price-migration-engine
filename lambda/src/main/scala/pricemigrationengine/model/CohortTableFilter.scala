package pricemigrationengine.model

sealed trait CohortTableFilter { val value: String }

// When adding a state, remember to update 'all'.
object CohortTableFilter {

  // ++++++++++ Normal states ++++++++++

  case object ReadyForEstimation extends CohortTableFilter { override val value: String = "ReadyForEstimation" }

  case object EstimationComplete extends CohortTableFilter { override val value: String = "EstimationComplete" }

  case object SalesforcePriceRiceCreationComplete extends CohortTableFilter {
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

  /*
   * Status of a sub that has been cancelled since the price migration process began,
   * so is ineligible for further processing.
   */
  case object Cancelled extends CohortTableFilter { override val value: String = "Cancelled" }

  /*
   * Status of a sub where the estimation indicates that its price will not increase,
   * so is ineligible for further processing.
   */
  case object NoPriceIncrease extends CohortTableFilter { override val value: String = "NoPriceIncrease" }

  /*
   * Status of a sub where the estimation indicates a price rise of 20% or more,
   * It was introduced during the Guardian Weekly price-rise that we would cap price-rises at 20%.
   * We need to do more dev work to add ChargeOverrides to cap the price when adding the rate plan to Zuora,
   * but we need to start the Cohort now, so are using this field temporarily to stop those subscriptions from being further processed.
   */
  case object CappedPriceIncrease extends CohortTableFilter { override val value: String = "CappedPriceIncrease" }

  // ++++++++++ Exceptional states ++++++++++

  case object EstimationFailed extends CohortTableFilter { override val value: String = "EstimationFailed" }

  case object NotificationSendProcessingOrError extends CohortTableFilter {
    override val value: String = "NotificationSendProcessingOrError"
  }

  case object AmendmentFailed extends CohortTableFilter { override val value: String = "AmendmentFailed" }

  // Set of all states.  Remember to update when adding a state.
  val all: Set[CohortTableFilter] = Set(
    AmendmentComplete,
    AmendmentFailed,
    AmendmentWrittenToSalesforce,
    Cancelled,
    EstimationComplete,
    EstimationFailed,
    NoPriceIncrease,
    NotificationSendComplete,
    NotificationSendDateWrittenToSalesforce,
    NotificationSendProcessingOrError,
    ReadyForEstimation,
    SalesforcePriceRiceCreationComplete
  )
}
