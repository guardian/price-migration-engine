package pricemigrationengine.model

object AmendmentHandlerHelper {
  def subscriptionHasCorrectBillingPeriodAfterUpdate(
      billingPeriodReferenceOpt: Option[String],
      subscriptionAfterUpdate: ZuoraSubscription,
      invoicePreviewAfterUpdate: ZuoraInvoiceList
  ): Option[Boolean] = {
    for {
      billingPeriodReference <- billingPeriodReferenceOpt
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(subscriptionAfterUpdate, invoicePreviewAfterUpdate)
      billingPeriodAfterUpdate <- SI2025Extractions.determineBillingPeriod(ratePlan)
    } yield billingPeriodReference == BillingPeriod.toString(billingPeriodAfterUpdate)
  }

  def priceEquality(float1: BigDecimal, float2: BigDecimal): Boolean = {
    (float1 - float2).abs < 0.001
  }
}
