package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.services.Zuora
import zio.{Clock, IO, Random, ZIO}

import java.time.LocalDate

object GW2024Migration {

  // ------------------------------------------------
  // Static Data
  // ------------------------------------------------

  val maxLeadTime = 49
  val minLeadTime = 36

  val priceMapMonthlies: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(15),
    "USD" -> BigDecimal(30),
    "CAD" -> BigDecimal(33),
    "AUD" -> BigDecimal(40),
    "NZD" -> BigDecimal(50),
    "EUR" -> BigDecimal(26.5),
    "ROW (USD)" -> BigDecimal(33)
  )

  val priceMapQuarterlies: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(45),
    "USD" -> BigDecimal(90),
    "CAD" -> BigDecimal(99),
    "AUD" -> BigDecimal(120),
    "NZD" -> BigDecimal(150),
    "EUR" -> BigDecimal(79.5),
    "ROW (USD)" -> BigDecimal(99)
  )

  val priceMapAnnuals: Map[String, BigDecimal] = Map(
    "GBP" -> BigDecimal(180),
    "USD" -> BigDecimal(360),
    "CAD" -> BigDecimal(396),
    "AUD" -> BigDecimal(480),
    "NZD" -> BigDecimal(600),
    "EUR" -> BigDecimal(318),
    "ROW (USD)" -> BigDecimal(396)
  )

  def getNewPrice1(billingPeriod: BillingPeriod, currency: Currency): Option[BigDecimal] = {
    billingPeriod match {
      case Monthly   => priceMapMonthlies.get(currency)
      case Quarterly => priceMapQuarterlies.get(currency)
      case Annual    => priceMapAnnuals.get(currency)
      case _         => None
    }
  }

  // ------------------------------------------------
  // Data Extraction and Manipulation
  // ------------------------------------------------

  def subscriptionToMigrationRatePlan(subscription: ZuoraSubscription): Option[ZuoraRatePlan] = {
    // This function tends to be implemented in each migration and the main reason
    // is that the name of the rate plan we are looking for is migration dependent
    ???
  }

  def subscriptionToMigrationCurrency(
      subscription: ZuoraSubscription,
      account: ZuoraAccount
  ): Option[Currency] = {
    for {
      ratePlan <- subscriptionToMigrationRatePlan(subscription)
      currency <- ZuoraRatePlan.ratePlanToCurrency(ratePlan: ZuoraRatePlan)
    } yield currency
  }

  def isROW(subscription: ZuoraSubscription, account: ZuoraAccount): Option[Boolean] = {
    for {
      ratePlan <- subscriptionToMigrationRatePlan(subscription: ZuoraSubscription)
      currency <- ZuoraRatePlan.ratePlanToCurrency(ratePlan: ZuoraRatePlan)
    } yield {
      val country = account.soldToContact.country
      currency == "USD" && country != "United States"
    }
  }

  def subscriptionToMigrationExtendedCurrency(
      subscription: ZuoraSubscription,
      account: ZuoraAccount
  ): Option[Currency] = {
    for {
      currency <- subscriptionToMigrationCurrency(subscription, account)
      isROW <- isROW(subscription: ZuoraSubscription, account: ZuoraAccount)
    } yield if (isROW) "ROW (USD)" else currency
  }

  def subscriptionToBillingPeriod(subscription: ZuoraSubscription): Option[BillingPeriod] = {
    for {
      ratePlan <- subscriptionToMigrationRatePlan(subscription)
      billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan)
    } yield billingPeriod
  }

  def getNewPrice2(subscription: ZuoraSubscription, account: ZuoraAccount): Option[BigDecimal] = {
    for {
      billingPeriod <- subscriptionToBillingPeriod(subscription)
      extendedCurrency <- subscriptionToMigrationExtendedCurrency(subscription, account)
      price <- getNewPrice1(billingPeriod, extendedCurrency)
    } yield price
  }

  def priceData(
      subscription: ZuoraSubscription,
      account: ZuoraAccount
  ): Either[AmendmentDataFailure, PriceData] = {
    val priceDataOpt = for {
      currency <- subscriptionToMigrationCurrency(subscription, account)
      ratePlan <- subscriptionToMigrationRatePlan(subscription)
      oldPrice = ZuoraRatePlan.ratePlanToRatePlanPrice(ratePlan)
      newPrice <- getNewPrice2(subscription, account)
      billingPeriod <- subscriptionToBillingPeriod(subscription)
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None =>
        Left(AmendmentDataFailure(s"Could not determine PriceData for subscription ${subscription.subscriptionNumber}"))
    }
  }

  def updateOfRatePlansToCurrent(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {
    for {
      update <- Left(
        AmendmentDataFailure(
          s"TBD"
        )
      )
    } yield update
  }

  // ------------------------------------------------
  // Effects
  // ------------------------------------------------
}
