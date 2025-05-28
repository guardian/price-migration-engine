package pricemigrationengine.libs

import pricemigrationengine.model._

import java.time.LocalDate

/*

  Date: 27th May 2025
  Author: Pascal

  SubscriptionIntrospection2025 was introduced in May 2025, in preparation of the
  Guardian Weekly and Newspaper, migrations starting soon to help improve the migration
  specific codes.

  "Modern" migrations use libraries to perform most of the legwork, but a bit needs
  to be implemented manually to match specific Marketing requests and, among other things implement
  the price grid (Pascal prefers hardcoding the price grid of each migration, notably in cases
  the prices are not yet, or identical, to the price catalogue)

  The purpose of SubscriptionIntrospection2025 is to facilitate writing the migration specific code
  by abstracting away some helpers functions we have (re)written a few times over the years

 */

object SubscriptionIntrospection2025 {

  /*
    Date: 28 May 2025
    Author: Pascal

    Function `invoicePreviewToChargeNumber` is the core of the logic to determine the migration
    rate plan (and the core of SubscriptionIntrospection2025). It takes an invoice list
    (something that we naturally have access to during Estimation) and extracts a charge number.
    The charge number will then be used to identify the migration rate plan.
   */

  def invoicePreviewToChargeNumber(invoiceList: ZuoraInvoiceList): Option[String] =
    invoiceList.invoiceItems.headOption.map(invoiceItem => invoiceItem.chargeNumber)

  /*
    Given a subscription and the rate plan charge we extracted from reading the subscription's invoice preview
    We can now identify the migration rate plan using `ratePlanChargeNumberToMatchingRatePlan`
   */

  def ratePlanChargeNumberToMatchingRatePlan(
      subscription: ZuoraSubscription,
      ratePlanChargeNumber: String
  ): Option[ZuoraRatePlan] =
    subscription.ratePlans.find(_.ratePlanCharges.exists(_.number == ratePlanChargeNumber))

  // --------------------------------------
  // Derivative attributes

  def determineCurrency(
      ratePlan: ZuoraRatePlan
  ): Option[Currency] = {
    ZuoraRatePlan.ratePlanToCurrency(ratePlan)
  }

  def determineBillingPeriod(
      ratePlan: ZuoraRatePlan
  ): Option[BillingPeriod] = ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan)

  def determineOldPrice(ratePlan: ZuoraRatePlan): BigDecimal = {
    ratePlan.ratePlanCharges.foldLeft(BigDecimal(0))((price: BigDecimal, ratePlanCharge: ZuoraRatePlanCharge) =>
      price + ratePlanCharge.price.getOrElse(BigDecimal(0))
    )
  }

  def determineNewPrice(): BigDecimal = {
    // This function is a placeholder for consistency to remind the engineer that
    // by convention the new prices are hard coded in the migration itself
    throw new Exception("[6170c05c] this function should not be called. See code comment.")
  }

  // --------------------------------------
  // priceData template

  /*
    The function below is a template, which needs to be copied into the migration module
    and adapted accordingly (for instance you might want to implement `determineNewPrice()`)

    It is only used in the test suite
   */

  def priceData(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList
  ): Either[DataExtractionFailure, PriceData] = {
    val priceDataOpt: Option[PriceData] = for {
      ratePlanChargeNumber <- invoicePreviewToChargeNumber(invoiceList)
      ratePlan <- ratePlanChargeNumberToMatchingRatePlan(subscription, ratePlanChargeNumber)
      currency <- determineCurrency(ratePlan)
      oldPrice = determineOldPrice(ratePlan: ZuoraRatePlan)
      newPrice = BigDecimal(
        2.71
      ) // Should replace this by a call to the migration's own `determineNewPrice()` the price grid lookup
      billingPeriod <- determineBillingPeriod(ratePlan)
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None =>
        Left(
          DataExtractionFailure(s"Could not determine PriceData for subscription ${subscription.subscriptionNumber}")
        )
    }
  }

}
