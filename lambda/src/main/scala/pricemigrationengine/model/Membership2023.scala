package pricemigrationengine.model
import pricemigrationengine.model.CohortSpec

import java.time.LocalDate

object Membership2023 {

  val priceMapMonthlies: Map[Currency, BigDecimal] = Map(
    "GBP" -> BigDecimal(7),
    "AUD" -> BigDecimal(14.99),
    "CAD" -> BigDecimal(12.99),
    "EUR" -> BigDecimal(9.99),
    "USD" -> BigDecimal(9.99),
  )

  val priceMapAnnuals: Map[Currency, BigDecimal] = Map(
    "GBP" -> BigDecimal(75),
    "AUD" -> BigDecimal(160),
    "CAD" -> BigDecimal(120),
    "EUR" -> BigDecimal(95),
    "USD" -> BigDecimal(120),
  )

  def priceData(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      nextServiceDate: LocalDate,
      cohortSpec: CohortSpec
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

    def currencyToNewPriceMonthlies(currency: String): Either[AmendmentDataFailure, BigDecimal] = {
      priceMapMonthlies.get(currency) match {
        case None => Left(AmendmentDataFailure(s"Could not determine a new monthly price for currency: ${currency}"))
        case Some(price) => Right(price)
      }
    }

    def currencyToNewPriceAnnuals(currency: String): Either[AmendmentDataFailure, BigDecimal] = {
      priceMapAnnuals.get(currency) match {
        case None => Left(AmendmentDataFailure(s"Could not determine a new annual price for currency: ${currency}"))
        case Some(price) => Right(price)
      }
    }

    MigrationType(cohortSpec) match {
      case Membership2023Monthlies =>
        for {
          ratePlan <- subscriptionRatePlan(subscription)
          ratePlanCharge <- subscriptionRatePlanCharge(subscription, ratePlan)
          currency = ratePlanCharge.currency
          oldPrice <- getOldPrice(subscription, ratePlanCharge)
          newPrice <- currencyToNewPriceMonthlies(currency: String)
        } yield PriceData(currency, oldPrice, newPrice, "Month")
      case Membership2023Annuals =>
        for {
          ratePlan <- subscriptionRatePlan(subscription)
          ratePlanCharge <- subscriptionRatePlanCharge(subscription, ratePlan)
          currency = ratePlanCharge.currency
          oldPrice <- getOldPrice(subscription, ratePlanCharge)
          newPrice <- currencyToNewPriceAnnuals(currency: String)
        } yield PriceData(currency, oldPrice, newPrice, "Annual")
      case _ => Left(AmendmentDataFailure(s"(error: 7ba45f10) Incorrect cohort spec for this function: ${cohortSpec}"))
    }
  }
}
