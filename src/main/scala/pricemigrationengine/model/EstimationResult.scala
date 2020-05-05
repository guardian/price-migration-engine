package pricemigrationengine.model

import java.time.LocalDate

case class EstimationResult(subscriptionName: String, expectedStartDate: LocalDate, estimatedNewPrice: Double)

object EstimationResult {

  def apply(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      earliestStartDate: LocalDate
  ): Either[AmendmentDataFailure, EstimationResult] =
    AmendmentData(subscription, invoiceList, earliestStartDate) map { amendmentData =>
      EstimationResult(subscription.subscriptionNumber, amendmentData.startDate, amendmentData.newPrice)
    }
}
