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

  def newPriceHasBeenCappedAt20Percent(oldPrice: BigDecimal, newPrice: BigDecimal): Boolean = {
    // This function takes a oldPrice (assumed from the cohort item) and a new price (assumed
    // from Zuora post amendment) and return true if the newPrice appears to have been capped at +20%

    // Note: This is Phase 1 of a solution to prevent unnecessarily alarming when a subscription has been
    // capped and the cohort item estimated newPrice (which didn't carry the capping) is not equal to
    // post amendment price. In Phase 2 we are going to expand the standard cohort item to add the
    // capped price (the price that as put into the comms). Phase 2 will come shortly.

    priceEquality(oldPrice * 1.2, newPrice)
  }

}
