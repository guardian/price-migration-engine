package pricemigrationengine.model

sealed trait CohortTableFilter { val value: String }

object CohortTableFilter {
  case object ReadyForEstimation extends CohortTableFilter { override val value: String = "ReadyForEstimation" }
  case object EstimationComplete extends CohortTableFilter { override val value: String = "EstimationComplete" }
  case object SalesforcePriceRiceCreationComplete extends CohortTableFilter {
    override val value: String = "SalesforcePriceRiceCreationComplete"
  }
  case object AmendmentComplete extends CohortTableFilter { override val value: String = "AmendmentComplete" }

  /*
   * Status of a sub that has been cancelled since the price migration process began,
   * so is ineligible for further processing.
   */
  case object Cancelled extends CohortTableFilter { override val value: String = "Cancelled" }
}
