package pricemigrationengine.model

import java.time.LocalDate

sealed trait ResultOfEstimation {
  val subscriptionName: String
}

object ResultOfEstimation {

  def apply(
      subscription: ZuoraSubscription,
      account: ZuoraAccount,
      earliestStartDate: LocalDate,
      currentDate: LocalDate
  ): ResultOfEstimation =
    AmendmentData(subscription, account, earliestStartDate, currentDate) match {
      case Left(failure) => EstimationFailed(subscription.subscriptionNumber, failure)
      case Right(amendmentData) =>
        EstimationSucceeded(subscription.subscriptionNumber, amendmentData.startDate, amendmentData.newPrice)
    }
}

case class EstimationSucceeded(subscriptionName: String, expectedStartDate: LocalDate, estimatedNewPrice: Double)
    extends ResultOfEstimation

case class EstimationFailed(subscriptionName: String, failure: Failure) extends ResultOfEstimation
