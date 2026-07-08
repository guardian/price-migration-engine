package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

import scala.util.Random

case class SP2026EmailExtraAttributes(
    contributionAmount: BigDecimal,
    currentCombinedAmount: BigDecimal,
    newCombinedAmount: BigDecimal
)

object SupporterPlus2026Migration {

  // ------------------------------------------------
  // Notification Timings
  // ------------------------------------------------

  val maxLeadTime = 35
  val minLeadTime = 33

  // ------------------------------------------------
  // Price Grid
  // ------------------------------------------------

  val priceGridNewPrices: Map[(BillingPeriod, String), BigDecimal] = Map(
    (Monthly, "GBP") -> BigDecimal(14.0),
    (Monthly, "USD") -> BigDecimal(18.0),
    (Monthly, "EUR") -> BigDecimal(14.0),
    (Monthly, "AUD") -> BigDecimal(25.0),
    (Monthly, "CAD") -> BigDecimal(18.0),
    (Monthly, "NZD") -> BigDecimal(25.0),
    (Annual, "GBP") -> BigDecimal(140.0),
    (Annual, "USD") -> BigDecimal(180.0),
    (Annual, "EUR") -> BigDecimal(140.0),
    (Annual, "AUD") -> BigDecimal(250.0),
    (Annual, "CAD") -> BigDecimal(180.0),
    (Annual, "NZD") -> BigDecimal(250.0),
  )

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  def monthliesOverSixWeeks(cursorDate: LocalDate, billingPeriod: BillingPeriod): LocalDate = {
    // This function takes a migration date and  billing period and either return the same
    // migration date, or return the migration date plus a number of months randomly chosen between
    // 0 and 1. This is to ensure that the migration of monthlies is done over 6 weeks.

    // The notifications are starting on 15 July. If we migrated all the monthlies over 1 month,
    // the monthly price rise dates would be between 19 August and 18th September
    // If the migration date falls between 19 August and 30th August, we make a random choice and then
    // possibly add one month.

    (cursorDate, billingPeriod) match {
      case (_, Annual)                                          => cursorDate
      case (date, _) if date.isAfter(LocalDate.of(2026, 8, 31)) => date
      case (date, _)                                            => {
        val shift = Random.nextInt(2) // decide a random integer in the interval [0, 1]
        date.plusMonths(shift)
      }
    }
  }

  def annualWithDiscountOneYearPolicy(cursorDate: LocalDate, subscription: ZuoraSubscription): LocalDate = {
    val activeDiscounts =
      SI2025Extractions.getActiveDiscountsPossiblyAfterEffectiveEndDate(subscription)
    if (activeDiscounts.isEmpty) {
      cursorDate
    } else {
      val maxEndDateOpt = activeDiscounts
        .flatMap(_.ratePlanCharges.map(_.effectiveEndDate))
        .max
      maxEndDateOpt match {
        case Some(date) => date.plusMonths(12)
        case None       => cursorDate
      }
    }
  }

  def subscriptionToContributionAmount(subscription: ZuoraSubscription): Option[BigDecimal] = {
    for {
      ratePlan <- subscription.ratePlans
        .filter(ratePlan => ZuoraRatePlan.ratePlanIsActive(ratePlan))
        .find(ratePlan => ratePlan.productName == "Supporter Plus")
      ratePlanCharge <- ratePlan.ratePlanCharges.find(rpc => rpc.name == "Contribution")
      price <- ratePlanCharge.price
    } yield price
  }

  def extractEmailExtraAttributes(
      subscription: ZuoraSubscription,
      cohortItem: CohortItem
  ): Option[SP2026EmailExtraAttributes] = {
    for {
      contributionAmount <- subscriptionToContributionAmount(subscription)
      currentBaseAmount <- cohortItem.oldPrice
      currentCombinedAmount = currentBaseAmount + contributionAmount
      futureBaseAmount <- cohortItem.commsPrice
      futureCombinedAmount = futureBaseAmount + contributionAmount
    } yield SP2026EmailExtraAttributes(
      contributionAmount,
      currentCombinedAmount,
      futureCombinedAmount
    )
  }

  // ------------------------------------------------
  // Primary Functions:
  //
  // The primary functions are the main functions that
  // are implemented by the *Migration module.
  //
  // - priceData is used in the Estimation handler
  // - amendmentOrderPayload is used in the Amendment handler
  // ------------------------------------------------

  def logValue[T](label: String)(value: T): T = {
    println(s"$label: $value")
    value
  }

  def determineOldPrice(ratePlan: ZuoraRatePlan): BigDecimal = {
    ratePlan.ratePlanCharges
      .filter(rpc => rpc.name != "Contribution")
      .foldLeft(BigDecimal(0))((price: BigDecimal, ratePlanCharge: ZuoraRatePlanCharge) =>
        price + ratePlanCharge.price.getOrElse(BigDecimal(0))
      )
  }

  def priceData(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
  ): Either[DataExtractionFailure, PriceData] = {
    val priceDataOpt = for {
      ratePlan <- SI2025RateplanFromSubAndInvoices
        .determineRatePlan(subscription, invoiceList)
        .map(logValue("ratePlan"))
      currency <- SI2025Extractions.determineCurrency(ratePlan).map(logValue("currency"))
      billingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan).map(logValue("billingPeriod"))
      oldPrice = logValue("oldPrice")(determineOldPrice(ratePlan))
      newPrice <- priceGridNewPrices.get((billingPeriod, currency)).map(logValue("newPrice"))
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None            =>
        Left(
          DataExtractionFailure(
            s"[e0254802] Could not determine PriceData for subscription ${subscription.subscriptionNumber}"
          )
        )
    }
  }

  def amendmentOrderPayload(cohortItem: CohortItem): Either[Failure, Value] = {
    // not yet implemented
    ???
  }
}
