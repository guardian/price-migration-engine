package pricemigrationengine.model

import java.time.LocalDate

case class EstimationData(
    subscriptionName: String,
    amendmentEffectiveDate: LocalDate,
    currency: Currency,
    oldPrice: BigDecimal,
    newPriceFull: BigDecimal,
    commsPrice: BigDecimal, // typically either the price grid new price, or that with capping
    billingPeriod: String
)

object EstimationData {
  def apply(
      account: ZuoraAccount,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      amendmentEffectiveDateLowerBound: LocalDate,
      cohortSpec: CohortSpec,
      today: LocalDate
  ): Either[Failure, EstimationData] = {
    for {
      amendmentEffectiveDate <- AmendmentData.nextServiceStartDate(
        invoiceList,
        subscription,
        amendmentEffectiveDateLowerBound
      )
      priceData <- AmendmentData.priceData(account, subscription, cohortSpec, invoiceList, today)
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
