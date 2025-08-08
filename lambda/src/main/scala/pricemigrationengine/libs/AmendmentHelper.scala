package pricemigrationengine.libs

import pricemigrationengine.libs.SI2025Extractions
import pricemigrationengine.libs.SI2025RateplanFromSubAndInvoices
import pricemigrationengine.model.{BillingPeriod, ZuoraInvoiceList, ZuoraRatePlan, ZuoraSubscription}

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
