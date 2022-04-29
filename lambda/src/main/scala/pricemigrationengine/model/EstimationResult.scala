package pricemigrationengine.model

import java.time.LocalDate

trait EstimationResult

case class SuccessfulEstimationResult(
    subscriptionName: String,
    startDate: LocalDate,
    currency: Currency,
    oldPrice: BigDecimal,
    estimatedNewPrice: BigDecimal,
    billingPeriod: String
) extends EstimationResult

object EstimationResult {

  def apply(
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      earliestStartDate: LocalDate
  ): Either[AmendmentDataFailure, SuccessfulEstimationResult] =
    AmendmentData(catalogue, subscription, invoiceList, earliestStartDate) map { amendmentData =>
      SuccessfulEstimationResult(
        subscription.subscriptionNumber,
        amendmentData.startDate,
        amendmentData.priceData.currency,
        amendmentData.priceData.oldPrice,
        amendmentData.priceData.newPrice,
        amendmentData.priceData.billingPeriod
      )
    }
}

case class FailedEstimationResult(subscriptionNumber: String, reason: String) extends EstimationResult

case class CancelledEstimationResult(subscriptionNumber: String) extends EstimationResult
