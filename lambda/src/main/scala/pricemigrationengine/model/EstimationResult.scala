package pricemigrationengine.model

import java.time.LocalDate

case class EstimationResult(
    subscriptionName: String,
    startDate: LocalDate,
    currency: Currency,
    oldPrice: BigDecimal,
    estimatedNewPrice: BigDecimal,
    billingPeriod: String
)

object EstimationResult {

  def apply(
      newProductPricing: ZuoraPricingData,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      earliestStartDate: LocalDate
  ): Either[AmendmentDataFailure, EstimationResult] =
    AmendmentData(newProductPricing, subscription, invoiceList, earliestStartDate) map { amendmentData =>
      EstimationResult(
        subscription.subscriptionNumber,
        amendmentData.startDate,
        amendmentData.priceData.currency,
        amendmentData.priceData.oldPrice,
        amendmentData.priceData.newPrice,
        amendmentData.priceData.billingPeriod
      )
    }
}
