package pricemigrationengine.model

import java.time.LocalDate

sealed trait ResultOfEstimation {
  val subscriptionName: String
}

object ResultOfEstimation {

  def fromSubscription(subscription: ZuoraSubscription): ResultOfEstimation =
    AmendmentData(subscription, LocalDate.now) match {
      case Left(failure) => EstimationFailed(subscription.name, failure)
      case Right(amendmentData) =>
        EstimationSucceeded(subscription.name, amendmentData.startDate, amendmentData.newPrice)
    }
}

case class EstimationSucceeded(subscriptionName: String, expectedStartDate: LocalDate, estimatedNewPrice: Double)
    extends ResultOfEstimation

case class EstimationFailed(subscriptionName: String, failure: Failure) extends ResultOfEstimation
