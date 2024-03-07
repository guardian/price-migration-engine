package pricemigrationengine.model

import java.time.LocalDate

trait EstimationResult

case class EstimationData(
    subscriptionName: String,
    startDate: LocalDate,
    currency: Currency,
    oldPrice: BigDecimal,
    estimatedNewPrice: BigDecimal,
    billingPeriod: String
) extends EstimationResult

case class FailedEstimationResult(subscriptionNumber: String, reason: String) extends EstimationResult

case class CancelledEstimationResult(subscriptionNumber: String) extends EstimationResult

object EstimationResult {
  def apply(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      startDateLowerBound: LocalDate,
      cohortSpec: CohortSpec,
  ): Either[AmendmentDataFailure, EstimationData] = {
    AmendmentData(account, catalogue, subscription, invoiceList, startDateLowerBound, cohortSpec) map { amendmentData =>
      EstimationData(
        subscription.subscriptionNumber,
        amendmentData.startDate,
        amendmentData.priceData.currency,
        amendmentData.priceData.oldPrice,
        amendmentData.priceData.newPrice,
        amendmentData.priceData.billingPeriod
      )
    }
  }
}
