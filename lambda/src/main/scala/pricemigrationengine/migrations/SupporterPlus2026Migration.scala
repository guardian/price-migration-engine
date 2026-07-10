package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}
import pricemigrationengine.services.Zuora

import java.time.LocalDate
import ujson._
import upickle.default._
import zio.ZIO

import scala.util.Random

case class SP2026EmailExtraAttributes(
    contributionAmount: String,
    currentCombinedAmount: String,
    newCombinedAmount: String
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

  def computeAmendmentEffectiveDateLowerBound(
      lowerBound0: LocalDate,
      item: CohortItem,
      subscription: ZuoraSubscription
  ): LocalDate = {
    // For this migration we have the requirement to migrate the monthlies over 6 weeks,
    // which is a non standard calculation. The main function is implemented in
    // monthliesOverSixWeeks
    // and here we call it just for that migration

    val lowerBound1 = item.billingPeriod
      .map(bp => monthliesOverSixWeeks(lowerBound0, BillingPeriod.fromString(bp)))
      .getOrElse(lowerBound0)

    // We also have the requirement to apply a one year policy to annual subs with an active discount
    // We cannot price rise a sub within a year after the end of the discount.
    // Implemented in annualWithDiscountOneYearPolicy

    val lowerBound2 = item.billingPeriod.map(bp => BillingPeriod.fromString(bp)) match {
      case Some(Annual) => annualWithDiscountOneYearPolicy(lowerBound1, subscription)
      case _            => lowerBound1
    }

    lowerBound2
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
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      subscription: ZuoraSubscription,
  ): Option[SP2026EmailExtraAttributes] = {

    MigrationType(cohortSpec) match {
      case SupporterPlus2026 => {
        for {
          contributionAmount <- subscriptionToContributionAmount(subscription)
          currentBaseAmount <- cohortItem.oldPrice
          currentCombinedAmount = currentBaseAmount + contributionAmount
          futureBaseAmount <- cohortItem.commsPrice
          futureCombinedAmount = futureBaseAmount + contributionAmount
        } yield SP2026EmailExtraAttributes(
          contributionAmount.toString(),
          currentCombinedAmount.toString(),
          futureCombinedAmount.toString()
        )
      }
      case _ =>
        Some(
          // Date: 9th July 2026
          // For Tom reading this... Same as usual, I can't return a None
          // and the case class cannot have its fields defined as Options :)
          SP2026EmailExtraAttributes(
            "",
            "",
            ""
          )
        )
    }

  }

  def brazeName(cohortItem: CohortItem, subscription: ZuoraSubscription): Option[String] = {
    /*
        Canvases:

        Name       : SV_SP_PriceRiseMonthlyNoCont2026
        ID         : d7181793-e2b4-45fc-a3fa-2fdd274d6ca7
        Description: Monthly S+ without a contribution

        Name       : SV_SP_PriceRiseAnnualNoCont2026
        ID         : 302c16dd-a3a9-4d01-a01e-324fadee6d7c
        Description: Annual S+ without a contribution

        Name       : SV_SP_PriceRiseMonthlyWithCont2026
        ID         : 725a9aab-45d4-481f-aa91-ca539c3e6ffc
        Description: Monthly S+ with contribution

        Name       : SV_SP_PriceRiseAnnualWithCont2026
        ID         : 293edcd5-2db9-4296-bcac-c07b69b8fe8c
        Description: Annual S+ with contribution
     */

    for {
      billingPeriod <- cohortItem.billingPeriod
      contributionAmount <- subscriptionToContributionAmount(subscription)
      hasNonZeroContribution = contributionAmount > 0
    } yield {
      (billingPeriod, hasNonZeroContribution) match {
        case ("Month", false)  => "SV_SP_PriceRiseMonthlyNoCont2026"
        case ("Month", true)   => "SV_SP_PriceRiseMonthlyWithCont2026"
        case ("Annual", false) => "SV_SP_PriceRiseAnnualNoCont2026"
        case ("Annual", true)  => "SV_SP_PriceRiseAnnualWithCont2026"
        case _                 =>
          throw new Exception(s"[fb8cdf0e] unexpected case, cohort item: ${cohortItem}, subscription: ${subscription}")
      }
    }
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
