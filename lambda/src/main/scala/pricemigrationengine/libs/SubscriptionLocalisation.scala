package pricemigrationengine.libs

import pricemigrationengine.model._

import java.time.LocalDate

/*
  Date: 28th May 2025
  Author: Pascal

  Localisation is mostly a Guardian Weekly concept but it does show up regularly
  and I also just agreed with Marketing that the definition we used last year
  should become permanent. This object facilitates the determination.

  Subscriptions are 'Domestic' unless they are
    - USD paying with a delivery address not in the `United States`, or
    - GBP paying with a delivery address not in the `United Kingdom`

  It uses the same approach as SI2025: using the invoice preview to determine
  the rate plan and derive the currency. The location is read from the account.
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
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(
        subscription,
        invoiceList
      )
      currency <- SI2025Extractions.determineCurrency(ratePlan)
    } yield {
      val country = account.soldToContact.country
      val isROWUSD = currency == "USD" && country != "United States"
      val isROWGBP = currency == "GBP" && country != "United Kingdom"
      if (isROWUSD || isROWGBP) {
        RestOfWorld
      } else {
        Domestic
      }
    }
  }
}
