package pricemigrationengine.model

object AmendmentHelper {
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
}
