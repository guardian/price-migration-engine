package pricemigrationengine.model

import java.time.LocalDate

trait EstimationResult

// EstimationData carries the metadata that is the result of the estimation step, and
// that will be used to update the cohort item in the dynamo table. The two other outcome
// of an estimation attempt are FailedEstimationResult and CancelledEstimationResult

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
  ): Either[Failure, EstimationData] = {
    for {
      startDate <- AmendmentData.nextServiceStartDate(invoiceList, subscription, startDateLowerBound)
      priceData <- AmendmentData.priceData(account, catalogue, subscription, invoiceList, startDate, cohortSpec)
    } yield EstimationData(
      subscription.subscriptionNumber,
      startDate,
      priceData.currency,
      priceData.oldPrice,
      priceData.newPrice,
      priceData.billingPeriod
    )
  }
}
