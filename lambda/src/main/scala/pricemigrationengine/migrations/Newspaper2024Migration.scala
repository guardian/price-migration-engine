package pricemigrationengine.migrations
import pricemigrationengine.model._

import java.time.LocalDate

object Newspaper2024Migration {

  val maxLeadTime = 40
  val minLeadTime = 35

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

  // RatePlanDetails was introduced to help properly test the data gathering to build the PriceData
  // It turned out to be particularly useful for testing that the logic was correct
  // It is currently limited to Newspaper2024Migration, but could be generalised to other (future) migrations
  case class RatePlanDetails(
      ratePlan: ZuoraRatePlan,
      ratePlanName: String,
      billingPeriod: BillingPeriod,
      currency: String,
      currentPrice: BigDecimal
  )

  // We have an unusual scheduling for this migration and Newspaper2024BatchId is used to
  // decide the correct start date for each subscription.
  sealed trait Newspaper2024BatchId
  object MonthliesPart1 extends Newspaper2024BatchId // First batch of monthlies
  object MonthliesPart2 extends Newspaper2024BatchId // Second batch of monthlies
  object MoreThanMonthlies extends Newspaper2024BatchId // Quarterlies, Semi-Annuals and Annuals

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
    // We are doing a multi product migration. This function tries and retrieve the correct product given a
    // subscription.
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

  def subscriptionToBatchId(subscription: ZuoraSubscription): Either[String, Newspaper2024BatchId] = {
    val ratePlanDetails = (for {
      productName <- subscriptionToMigrationProductName(subscription)
      ratePlanDetails <- subscriptionToRatePlanDetails(subscription, productName)
    } yield ratePlanDetails)

    ratePlanDetails match {
      case Left(message) => Left(message)
      case Right(ratePlanDetails) => {
        ratePlanDetails.billingPeriod match {
          case Monthly => {
            val ratePlan = ratePlanDetails.ratePlan
            ratePlan.ratePlanCharges.toList match {
              case Nil => Left("")
              case rpc :: _ => {
                val monthIndex = rpc.chargedThroughDate.getOrElse(LocalDate.of(2024, 1, 1)).getDayOfMonth
                if (monthIndex <= 20) {
                  Right(MonthliesPart2)
                } else {
                  Right(MonthliesPart1)
                }
              }
            }
          }
          case _ => Right(MoreThanMonthlies)
        }
      }
    }
  }

  def batchIdToEarliestMigrationStartDate(batchId: Newspaper2024BatchId): LocalDate = {
    batchId match {
      case MonthliesPart1    => LocalDate.of(2024, 2, 21) // 21 Feb 2024
      case MonthliesPart2    => LocalDate.of(2024, 3, 18) // 18 March 2024
      case MoreThanMonthlies => LocalDate.of(2024, 3, 1) // 1 March 2024
    }
  }

  def subscriptionToEarliestMigrationStartDate(subscription: ZuoraSubscription): LocalDate = {
    subscriptionToBatchId(subscription) match {
      case Right(bid)   => batchIdToEarliestMigrationStartDate(bid)
      case Left(string) => LocalDate.of(2024, 4, 1)
      // Default date to avoid returning a more complex value than a LocalDate
    }
  }

  def startDateGeneralLowerbound(
      cohortSpec: CohortSpec,
      today: LocalDate,
      subscription: ZuoraSubscription
  ): LocalDate = {

    // Technically the startDateGeneralLowerbound is a function of the cohort spec and the notification min time.
    // The cohort spec carries the lowest date we specify there can be a price migration, and the notification min
    // time ensures the legally required lead time for customer communication. The max of those two dates is the date
    // from which we can realistically perform a price increase. With that said, other policies can apply, for
    // instance:
    // - The one year policy, which demand that we do not price rise customers during the subscription first year
    // - The spread: a mechanism, used for monthlies, by which we do not let a large number of monthlies migrate
    //   during a single month.

    // We expanded the signature of this function for the Newspaper2024 migration where that date was specific of
    // the subscription due to its un-unusual scheduling. For Newspaper2024 we call a specific function from the
    // migration support code.

    val earliestPriceMigrationStartDate = subscriptionToEarliestMigrationStartDate(subscription)

    def datesMax(date1: LocalDate, date2: LocalDate): LocalDate = if (date1.isBefore(date2)) date2 else date1

    datesMax(
      earliestPriceMigrationStartDate,
      today.plusDays(
        minLeadTime + 1
      ) // +1 because we need to be strictly over minLeadTime days away. Exactly minLeadTime is not enough.
    )
  }

  def startDateSpreadPeriod(subscription: ZuoraSubscription): Int = {
    subscriptionToBatchId(subscription) match {
      case Left(_) => 1
      case Right(bid) =>
        bid match {
          case MonthliesPart1    => 1
          case MonthliesPart2    => 2
          case MoreThanMonthlies => 1
        }
    }
  }

}
