package pricemigrationengine.migrations
import pricemigrationengine.model._

import java.time.LocalDate

object Newspaper2024Migration {

  /*
    Correspondence between product names in Salesforce versus Zuora

    -----------------------------------------------------------------
    Product name in the Salesforce export | Product name in Zuora
    -----------------------------------------------------------------
    Newspaper - Home Delivery             | Newspaper Delivery
    Newspaper - Subscription Card         | Newspaper Digital Voucher
    Newspaper - Voucher Book              | Newspaper Voucher
    -----------------------------------------------------------------
   */

  case class RatePlanDetails(
      ratePlan: ZuoraRatePlan,
      ratePlanName: String,
      billingPeriod: BillingPeriod,
      currency: String,
      currentPrice: BigDecimal
  )

  private val newspaperHomeDeliveryPricesMonthly: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(78.99),
    "Sixday" -> BigDecimal(68.99),
    "Weekend" -> BigDecimal(31.99),
    "Saturday" -> BigDecimal(19.99),
    "Sunday" -> BigDecimal(19.99),
    "Everyday+" -> BigDecimal(80.99),
    "Sixday+" -> BigDecimal(70.99),
    "Weekend+" -> BigDecimal(40.99),
    "Saturday+" -> BigDecimal(30.99),
    "Sunday+" -> BigDecimal(30.99),
  )

  private val newspaperHomeDeliveryPricesQuarterly: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(236.97),
    "Sixday" -> BigDecimal(206.97),
    "Weekend" -> BigDecimal(95.97),
    "Saturday" -> BigDecimal(59.97),
    "Sunday" -> BigDecimal(59.97),
  )

  private val newspaperSubscriptionCardMonthly: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(64.99),
    "Sixday" -> BigDecimal(56.99),
    "Weekend" -> BigDecimal(25.99),
    "Saturday" -> BigDecimal(14.99),
    "Sunday" -> BigDecimal(14.99),
    "Everyday+" -> BigDecimal(66.99),
    "Sixday+" -> BigDecimal(58.99),
    "Weekend+" -> BigDecimal(34.99),
    "Saturday+" -> BigDecimal(25.99),
    "Sunday+" -> BigDecimal(25.99),
  )

  private val newspaperSubscriptionCardQuarterly: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(194.97),
    "Sixday" -> BigDecimal(170.97),
    "Weekend" -> BigDecimal(77.97),
    "Everyday+" -> BigDecimal(200.97),
    "Sixday+" -> BigDecimal(176.97),
  )

  private val newspaperSubscriptionCardSemiAnnual: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(389.94),
    "Sixday" -> BigDecimal(341.94),
    "Everyday+" -> BigDecimal(401.94),
  )

  private val newspaperSubscriptionCardAnnual: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(779.88),
    "Sixday" -> BigDecimal(683.88),
    "Weekend" -> BigDecimal(311.88),
  )

  private val newspaperVoucherBookMonthly: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(64.99),
    "Sixday" -> BigDecimal(56.99),
    "Weekend" -> BigDecimal(25.99),
    "Saturday" -> BigDecimal(14.99),
    "Sunday" -> BigDecimal(14.99),
    "Everyday+" -> BigDecimal(66.99),
    "Sixday+" -> BigDecimal(58.99),
    "Weekend+" -> BigDecimal(34.99),
    "Saturday+" -> BigDecimal(25.99),
    "Sunday+" -> BigDecimal(25.99),
  )

  private val newspaperVoucherBookQuarterly: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(194.97),
    "Sixday" -> BigDecimal(170.97),
    "Weekend" -> BigDecimal(77.97),
    "Everyday+" -> BigDecimal(200.97),
    "Sixday+" -> BigDecimal(176.97),
    "Weekend+" -> BigDecimal(104.97),
    "Sunday+" -> BigDecimal(77.97),
  )

  private val newspaperVoucherBookSemiAnnual: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(389.94),
    "Sixday" -> BigDecimal(341.94),
    "Weekend" -> BigDecimal(155.94),
    "Everyday+" -> BigDecimal(401.94),
    "Sixday+" -> BigDecimal(353.94),
    "Weekend+" -> BigDecimal(209.94),
    "Sunday+" -> BigDecimal(155.94),
  )

  private val newspaperVoucherBookAnnual: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(779.88),
    "Sixday" -> BigDecimal(683.88),
    "Weekend" -> BigDecimal(311.88),
    "Everyday+" -> BigDecimal(803.88),
    "Sixday+" -> BigDecimal(707.88),
    "Weekend+" -> BigDecimal(419.88),
  )

  def priceLookup(product: String, billingPeriod: BillingPeriod, ratePlanName: String): Option[BigDecimal] = {
    val empty: Map[String, BigDecimal] = Map()
    val priceMap = (product, billingPeriod) match {
      case ("Newspaper Delivery", Monthly)           => newspaperHomeDeliveryPricesMonthly
      case ("Newspaper Delivery", Quarterly)         => newspaperHomeDeliveryPricesQuarterly
      case ("Newspaper Digital Voucher", Monthly)    => newspaperSubscriptionCardMonthly
      case ("Newspaper Digital Voucher", Quarterly)  => newspaperSubscriptionCardQuarterly
      case ("Newspaper Digital Voucher", SemiAnnual) => newspaperSubscriptionCardSemiAnnual
      case ("Newspaper Digital Voucher", Annual)     => newspaperSubscriptionCardAnnual
      case ("Newspaper Voucher", Monthly)            => newspaperVoucherBookMonthly
      case ("Newspaper Voucher", Quarterly)          => newspaperVoucherBookQuarterly
      case ("Newspaper Voucher", SemiAnnual)         => newspaperVoucherBookSemiAnnual
      case ("Newspaper Voucher", Annual)             => newspaperVoucherBookAnnual
      case _                                         => empty
    }
    priceMap.get(ratePlanName)
  }

  def subscriptionToMigrationProductName(subscription: ZuoraSubscription): Either[String, String] = {
    val migrationProductNames = List("Newspaper Delivery", "Newspaper Digital Voucher", "Newspaper Voucher")
    val names = subscription.ratePlans
      .filter(ratePlan => ratePlan.lastChangeType == None || ratePlan.lastChangeType == Some("Add"))
      .map(ratePlan => ratePlan.productName)
      .filter(name => migrationProductNames.contains(name))
      .distinct
    names match {
      case Nil =>
        Left(
          s"[error: d5fb6922] could not determine migration product name for subscription ${subscription.subscriptionNumber}; no name to choose from"
        )
      case name :: Nil => Right(name)
      case _ =>
        Left(
          s"[error: d3ecd18d] could not determine migration product name for subscription ${subscription.subscriptionNumber}; more than one name to choose from"
        )
    }
  }

  def ratePlanToBillingPeriod(ratePlan: ZuoraRatePlan): Option[BillingPeriod] = {
    for {
      ratePlanCharge <- ratePlan.ratePlanCharges.headOption
      billingPeriod <- ratePlanCharge.billingPeriod
    } yield BillingPeriod.fromString(billingPeriod)
  }

  def ratePlanToCurrency(ratePlan: ZuoraRatePlan): Option[String] = {
    for {
      ratePlanCharge <- ratePlan.ratePlanCharges.headOption
    } yield ratePlanCharge.currency
  }

  def subscriptionToRatePlanDetails(
      subscription: ZuoraSubscription,
      productName: String
  ): Either[String, RatePlanDetails] = {
    val ratePlans = {
      subscription.ratePlans
        .filter(ratePlan => ratePlan.productName == productName)
        .filter(ratePlan => ratePlan.lastChangeType == None || ratePlan.lastChangeType == Some("Add"))
        .distinct
    }
    ratePlans match {
      case Nil =>
        Left(
          s"[error 93a21a48] Subscription ${subscription.subscriptionNumber} was found to have zero newsPaperDeliveryRatePlans making determination of rate plan name impossible"
        )
      case ratePlan :: Nil => {
        (for {
          billingPeriod <- ratePlanToBillingPeriod(ratePlan)
          currency <- ratePlanToCurrency(ratePlan)
          currentPrice = ratePlan.ratePlanCharges.foldLeft(BigDecimal(0))(
            (price: BigDecimal, ratePlanCharge: ZuoraRatePlanCharge) =>
              price + ratePlanCharge.price.getOrElse(BigDecimal(0))
          )
        } yield RatePlanDetails(
          ratePlan,
          ratePlan.ratePlanName,
          billingPeriod,
          currency,
          currentPrice
        )) match {
          case Some(data) => Right(data)
          case _ => Left(s"[error: 0e218c37] Could not determine billing period for subscription ${subscription}")
        }
      }
      case _ =>
        Left(
          s"[error 93a21a48] Subscription ${subscription.subscriptionNumber} was found to have more than one newsPaperDeliveryRatePlans making determination of rate plan name impossible"
        )
    }
  }

  def subscriptionToNewPrice(subscription: ZuoraSubscription): Option[BigDecimal] = {
    for {
      productName <- subscriptionToMigrationProductName(subscription).toOption
      ratePlanDetails <- subscriptionToRatePlanDetails(subscription, productName).toOption
      price <- priceLookup(productName, ratePlanDetails.billingPeriod, ratePlanDetails.ratePlanName)
    } yield price
  }

  def priceData(
      subscription: ZuoraSubscription,
  ): Either[AmendmentDataFailure, PriceData] = {

    // PriceData(currency: Currency, oldPrice: BigDecimal, newPrice: BigDecimal, billingPeriod: String)

    def transform1[T](option: Option[T]): Either[AmendmentDataFailure, T] = {
      option match {
        case None                 => Left(AmendmentDataFailure("error"))
        case Some(ratePlanCharge) => Right(ratePlanCharge)
      }
    }

    def transform2[T](data: Either[String, T]): Either[AmendmentDataFailure, T] = {
      data match {
        case Left(string) => Left(AmendmentDataFailure(string))
        case Right(t)     => Right(t)
      }
    }

    for {
      productName <- transform2[String](subscriptionToMigrationProductName(subscription))
      ratePlanDetails <- transform2[RatePlanDetails](subscriptionToRatePlanDetails(subscription, productName))
      oldPrice = ratePlanDetails.currentPrice
      newPrice <- transform1[BigDecimal](subscriptionToNewPrice(subscription))
    } yield PriceData(
      ratePlanDetails.currency,
      oldPrice,
      newPrice,
      BillingPeriod.toString(ratePlanDetails.billingPeriod)
    )
  }
}
