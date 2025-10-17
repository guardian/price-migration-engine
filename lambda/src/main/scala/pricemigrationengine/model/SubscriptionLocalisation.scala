package pricemigrationengine.model

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

    // Date: October 2025
    // Author: Pascal

    // This currently implement a restrictive definition of ROW.
    // See [What does ROW (Rest of World) means](docs/ROW-definition.md)
    // for the full definition

    // Note that GW2025 was actually using a much better definition (one covering USD and GBP)
    // which has been moved to the GW2025 own's code
    // here: https://github.com/guardian/price-migration-engine/pull/1275

    // If one day another migration needs the multiple currencies version of the definition,
    // then we should update this one, but then we should probably update the signature of
    // the function so that it returns a currency and the localization

    for {
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(
        subscription,
        invoiceList
      )
      currency <- SI2025Extractions.determineCurrency(ratePlan)
    } yield {
      if (currency == "USD" && account.soldToContact.country != "United States") {
        RestOfWorld
      } else {
        Domestic
      }
    }
  }
}
