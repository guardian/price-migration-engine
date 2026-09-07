package pricemigrationengine.model

import java.time.LocalDate

trait EstimationResult

// EstimationData carries the metadata that is the result of the estimation step, and
// that will be used to update the cohort item in the dynamo table. The two other outcome
// of an estimation attempt are FailedEstimationResult and CancelledEstimationResult

case class EstimationData(
    subscriptionName: String,
    amendmentEffectiveDate: LocalDate,
    currency: Currency,
    oldPrice: BigDecimal,
    newPriceFull: BigDecimal,
    commsPrice: BigDecimal, // typically either the price grid new price, or that with capping
    billingPeriod: String
) extends EstimationResult

object EstimationResult {
  def apply(
      account: ZuoraAccount,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      amendmentEffectiveDateLowerBound: LocalDate,
      cohortSpec: CohortSpec,
  ): Either[Failure, EstimationData] = {
    for {
      amendmentEffectiveDate <- AmendmentData.nextServiceStartDate(
        invoiceList,
        subscription,
        amendmentEffectiveDateLowerBound
      )
      priceData <- AmendmentData.priceData(account, subscription, cohortSpec, invoiceList)
    } yield EstimationData(
      subscription.subscriptionNumber,
      amendmentEffectiveDate,
      priceData.currency,
      priceData.oldPrice,
      priceData.newPriceFull, // cohort Item's estimatedNewPrice
      priceData.commsPrice, // full price with possible capping
      priceData.billingPeriod
    )
  }
}

case class SubscriptionCancelledInZuoraEstimationResult(subscriptionNumber: String) extends EstimationResult
case class SubscriptionAutoRenewIsFalseEstimationResult(subscriptionNumber: String) extends EstimationResult
