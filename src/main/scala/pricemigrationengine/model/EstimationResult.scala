package pricemigrationengine.model

import java.time.LocalDate

case class EstimationResult(subscriptionName: String, expectedStartDate: LocalDate, estimatedNewPrice: Double)

object EstimationResult {

  def apply(
      subscription: ZuoraSubscription,
      account: ZuoraAccount,
      earliestStartDate: LocalDate,
      currentDate: LocalDate
  ): Either[AmendmentDataFailure, EstimationResult] =
    AmendmentData(subscription, account, earliestStartDate, currentDate) map { amendmentData =>
      EstimationResult(subscription.subscriptionNumber, amendmentData.startDate, amendmentData.newPrice)
    }
}
