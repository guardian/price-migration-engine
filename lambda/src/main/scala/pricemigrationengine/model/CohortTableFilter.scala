package pricemigrationengine.model

sealed trait CohortTableFilter { val value: String }

// When adding a state, remember to update 'all'.
object CohortTableFilter {

  // normal workflow states

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

  // other terminal states

  case object NoPriceIncrease extends CohortTableFilter { override val value: String = "NoPriceIncrease" }
  case object ZuoraCancellation extends CohortTableFilter { override val value: String = "ZuoraCancellation" }

  // exceptional states

  // UserOptOut was added for the October 2025 print product migration
  case object UserOptOut extends CohortTableFilter { override val value: String = "UserOptOut" }
  case object NotificationSendDateWrittenToSalesforceN4HOLD extends CohortTableFilter {
    override val value: String = "NotificationSendDateWrittenToSalesforceN4HOLD"
  }

  val allQueryableStates: Set[CohortTableFilter] = Set(
    ReadyForEstimation,
    EstimationComplete,
    SalesforcePriceRiseCreationComplete,
    NotificationSendComplete,
    NotificationSendDateWrittenToSalesforce,
    NotificationSendDateWrittenToSalesforceN4HOLD,
    AmendmentComplete
    // AmendmentWrittenToSalesforce (is the terminal state of a normal migration, but not queryable)
  )
}
