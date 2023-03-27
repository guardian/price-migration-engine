package pricemigrationengine.model
import java.time.LocalDate

object Membership2023 {

  /*
    Date: 20th March 2023
    Author: Pascal

    This object is currently written to support the first two batches (monthly) of the membership migration.
    Support for the third (annual) will be added shortly.
   */

  val priceMap: Map[Currency, BigDecimal] = Map(
    "GBP" -> BigDecimal(7),
    "AUD" -> BigDecimal(14),
    "CAD" -> BigDecimal(9.79),
    "EUR" -> BigDecimal(6.99),
    "USD" -> BigDecimal(9.79),
  )

  def priceData(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      nextServiceDate: LocalDate,
  ): Either[AmendmentDataFailure, PriceData] = {

    // Here we are going to use the Rate Plan from the subscription itself.
    // We do not need to look up the one in the product catalogue.
    // We will be using it to find out the currency.

    def subscriptionRatePlan(subscription: ZuoraSubscription): Either[AmendmentDataFailure, ZuoraRatePlan] = {
      subscription.ratePlans.headOption match {
        case None =>
          Left(AmendmentDataFailure(s"Subscription ${subscription.subscriptionNumber} doesn't have any rate plan"))
        case Some(ratePlan) => Right(ratePlan)
      }
    }

    def subscriptionRatePlanCharge(
        subscription: ZuoraSubscription,
        ratePlan: ZuoraRatePlan
    ): Either[AmendmentDataFailure, ZuoraRatePlanCharge] = {
      ratePlan.ratePlanCharges.headOption match {
        case None => {
          // Although not enforced by the signature of the function, for this error message to make sense we expect that
          // the rate plan belongs to the currency
          Left(
            AmendmentDataFailure(s"Subscription ${subscription.subscriptionNumber} has a rate plan, but with no charge")
          )
        }
        case Some(ratePlanCharge) => Right(ratePlanCharge)
      }
    }

    def getOldPrice(
        subscription: ZuoraSubscription,
        ratePlanCharge: ZuoraRatePlanCharge
    ): Either[AmendmentDataFailure, BigDecimal] = {
      ratePlanCharge.price match {
        case None => {
          // Although not enforced by the signature of the function, for this error message to make sense we expect that
          // the rate plan charge belongs to the currency
          Left(
            AmendmentDataFailure(
              s"Subscription ${subscription.subscriptionNumber} has a rate plan charge, but with no currency"
            )
          )
        }
        case Some(price) => Right(price)
      }

    }

    def currencyToNewPrice(currency: String): Either[AmendmentDataFailure, BigDecimal] = {
      priceMap.get(currency) match {
        case None        => Left(AmendmentDataFailure(s"Could not determine a new Price for currency: ${currency}"))
        case Some(price) => Right(price)
      }
    }

    for {
      ratePlan <- subscriptionRatePlan(subscription)
      ratePlanCharge <- subscriptionRatePlanCharge(subscription, ratePlan)
      currency = ratePlanCharge.currency
      oldPrice <- getOldPrice(subscription, ratePlanCharge)
      newPrice <- currencyToNewPrice(currency: String)
    } yield PriceData(currency, oldPrice, newPrice, "Month")
  }

}
