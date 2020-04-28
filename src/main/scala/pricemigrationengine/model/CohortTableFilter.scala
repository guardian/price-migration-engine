package pricemigrationengine.model

sealed trait CohortTableFilter

object CohortTableFilter {

  case object ReadyForEstimation extends CohortTableFilter
}
