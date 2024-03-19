package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.util._
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

  def getNewPrice(billingPeriod: BillingPeriod, extendedCurrency: Currency): Option[BigDecimal] = {
    billingPeriod match {
      case Monthly   => priceMapMonthlies.get(extendedCurrency)
      case Quarterly => priceMapQuarterlies.get(extendedCurrency)
      case Annual    => priceMapAnnuals.get(extendedCurrency)
      case _         => None
    }
  }

  // ------------------------------------------------
  // Data Extraction and Manipulation
  // ------------------------------------------------

  def subscriptionToMigrationRatePlans(subscription: ZuoraSubscription): List[ZuoraRatePlan] = {
    val migrationRatePlanNames = List(
      "GW Oct 18 - Monthly - Domestic",
      "GW Oct 18 - Quarterly - Domestic",
      "GW Oct 18 - Annual - Domestic",
      "GW Oct 18 - Monthly - ROW",
      "GW Oct 18 - Quarterly - ROW",
      "GW Oct 18 - Annual - ROW",
      "Guardian Weekly Quarterly",
      "Guardian Weekly Annual",
      "GW Oct 18 - 1 Year - Domestic",
      "Guardian Weekly 1 Year",
    )
    subscription.ratePlans.filter(rp => migrationRatePlanNames.contains(rp.ratePlanName))
  }

  def subscriptionToMigrationRatePlan(subscription: ZuoraSubscription): Option[ZuoraRatePlan] = {
    subscriptionToMigrationRatePlans(subscription: ZuoraSubscription).headOption
  }

  def subscriptionToCurrency(
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

  def subscriptionToExtendedCurrency(
      subscription: ZuoraSubscription,
      account: ZuoraAccount
  ): Option[Currency] = {
    for {
      currency <- subscriptionToCurrency(subscription, account)
      isROW <- isROW(subscription: ZuoraSubscription, account: ZuoraAccount)
    } yield if (isROW) "ROW (USD)" else currency
  }

  def subscriptionToBillingPeriod(subscription: ZuoraSubscription): Option[BillingPeriod] = {
    for {
      ratePlan <- subscriptionToMigrationRatePlan(subscription)
      billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan)
    } yield billingPeriod
  }

  def getNewPrice(subscription: ZuoraSubscription, account: ZuoraAccount): Option[BigDecimal] = {
    for {
      billingPeriod <- subscriptionToBillingPeriod(subscription)
      extendedCurrency <- subscriptionToExtendedCurrency(subscription, account)
      price <- getNewPrice(billingPeriod, extendedCurrency)
    } yield price
  }

  def subscriptionToLastPriceMigrationDate(subscription: ZuoraSubscription): Option[LocalDate] = {
    Some(
      subscriptionToMigrationRatePlans(subscription)
        .flatMap(ratePlan => ratePlan.ratePlanCharges)
        .flatMap(rpc => rpc.originalOrderDate)
        .foldLeft(LocalDate.of(2000, 1, 1))((acc, date) => Date.datesMax(acc, date))
    )
  }

  def priceData(
      subscription: ZuoraSubscription,
      account: ZuoraAccount
  ): Either[AmendmentDataFailure, PriceData] = {
    val priceDataOpt = for {
      currency <- subscriptionToCurrency(subscription, account)
      ratePlan <- subscriptionToMigrationRatePlan(subscription)
      oldPrice = ZuoraRatePlan.ratePlanToRatePlanPrice(ratePlan)
      newPrice <- getNewPrice(subscription, account)
      billingPeriod <- subscriptionToBillingPeriod(subscription)
    } yield PriceData(currency, oldPrice, newPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None =>
        Left(AmendmentDataFailure(s"Could not determine PriceData for subscription ${subscription.subscriptionNumber}"))
    }
  }

  def zuoraUpdate(
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
}
