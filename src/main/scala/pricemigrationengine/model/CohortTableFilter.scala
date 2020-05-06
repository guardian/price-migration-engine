package pricemigrationengine.model

sealed trait CohortTableFilter { val value: String }

object CohortTableFilter {
  case object ReadyForEstimation extends CohortTableFilter { override val value: String = "ReadyForEstimation" }
}
