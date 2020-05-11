package pricemigrationengine.model

sealed trait Failure {
  val reason: String
}

case class ConfigurationFailure(reason: String) extends Failure

case class CohortFetchFailure(reason: String) extends Failure
case class CohortUpdateFailure(reason: String) extends Failure

case class ZuoraFetchFailure(reason: String) extends Failure

case class AmendmentDataFailure(reason: String) extends Failure
