package pricemigrationengine.libs

import pricemigrationengine.model._

import java.time.LocalDate

/*

  Date: 28th May 2025
  Author: Pascal

  Localisation is mostly a Guardian Weekly concept but it does show up regularly
  and I also just agreed with Marketing that the definition we used last year
  should become permanent. This object facilitates the determination.

  It uses the same approach as SubscriptionIntrospection2025:
  using the invoice preview to determine the rate plan and derive the currency.
  The location is read from the account

 */

sealed trait SubscriptionLocalisation
object Domestic extends SubscriptionLocalisation
object RestOfWorld extends SubscriptionLocalisation

object SubscriptionLocalisation {
  def determineSubscriptionLocalisation(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      account: ZuoraAccount
  ): Option[SubscriptionLocalisation] = {
    for {
      ratePlanChargeNumber <- SubscriptionIntrospection2025.invoicePreviewToChargeNumber(invoiceList)
      ratePlan <- SubscriptionIntrospection2025.ratePlanChargeNumberToMatchingRatePlan(
        subscription,
        ratePlanChargeNumber
      )
      currency <- SubscriptionIntrospection2025.determineCurrency(ratePlan)
    } yield {
      val country = account.soldToContact.country
      val isROW = currency == "USD" && country != "United States"
      if (isROW) {
        RestOfWorld
      } else {
        Domestic
      }
    }
  }
}
