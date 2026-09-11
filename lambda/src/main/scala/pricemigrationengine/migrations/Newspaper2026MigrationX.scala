package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}

import java.time.LocalDate
import ujson._

import scala.math.BigDecimal.RoundingMode

sealed trait NxFulfillment
object Voucher extends NxFulfillment
object HomeDelivery extends NxFulfillment

sealed trait NxPackage
// The strange naming for the packages (rate plans) is because we are treating
// the `+` and `non +` rate plans the same.
// For instance (Monthly, VoucherCard, Everyday) and (Monthly, VoucherCard, Everyday+)
// both map to 72.99
object EverydayBasicAndPlus extends NxPackage
object SixdayBasicAndPlus extends NxPackage
object WeekendBasicAndPlus extends NxPackage
object SaturdayBasicAndPlus extends NxPackage
object EchoLegacy extends NxPackage

object Newspaper2026MigrationX {

  val priceGridNewPricesMonthlies: Map[(NxPackage, NxFulfillment), BigDecimal] = Map(
    (EverydayBasicAndPlus, Voucher) -> BigDecimal(72.99),
    (SixdayBasicAndPlus, Voucher) -> BigDecimal(64.99),
    (WeekendBasicAndPlus, Voucher) -> BigDecimal(29.99),
    (SaturdayBasicAndPlus, Voucher) -> BigDecimal(16.99),
    (EverydayBasicAndPlus, HomeDelivery) -> BigDecimal(88.99),
    (SixdayBasicAndPlus, HomeDelivery) -> BigDecimal(77.99),
    (WeekendBasicAndPlus, HomeDelivery) -> BigDecimal(36.99),
    (SaturdayBasicAndPlus, HomeDelivery) -> BigDecimal(21.99),
  )
  val priceGridNewPricesQuarterlies: Map[(NxPackage, NxFulfillment), BigDecimal] = Map(
    (EverydayBasicAndPlus, Voucher) -> BigDecimal(218.97),
    (SixdayBasicAndPlus, Voucher) -> BigDecimal(194.97),
    (WeekendBasicAndPlus, Voucher) -> BigDecimal(89.97),
    (SaturdayBasicAndPlus, Voucher) -> BigDecimal(50.97),
    (EverydayBasicAndPlus, HomeDelivery) -> BigDecimal(266.97),
    (SixdayBasicAndPlus, HomeDelivery) -> BigDecimal(233.97),
    (WeekendBasicAndPlus, HomeDelivery) -> BigDecimal(110.97),
    (SaturdayBasicAndPlus, HomeDelivery) -> BigDecimal(65.97),
  )
  val priceGridNewPricesSemiannuals: Map[(NxPackage, NxFulfillment), BigDecimal] = Map(
    (EverydayBasicAndPlus, Voucher) -> BigDecimal(437.94),
    (SixdayBasicAndPlus, Voucher) -> BigDecimal(389.94),
    (WeekendBasicAndPlus, Voucher) -> BigDecimal(179.9),
    (SaturdayBasicAndPlus, Voucher) -> BigDecimal(101.94),
    (EverydayBasicAndPlus, HomeDelivery) -> BigDecimal(533.94),
    (SixdayBasicAndPlus, HomeDelivery) -> BigDecimal(467.94),
    (WeekendBasicAndPlus, HomeDelivery) -> BigDecimal(221.94),
    (SaturdayBasicAndPlus, HomeDelivery) -> BigDecimal(131.94),
  )
  val priceGridNewPricesAnnuals: Map[(NxPackage, NxFulfillment), BigDecimal] = Map(
    (EverydayBasicAndPlus, Voucher) -> BigDecimal(875.88),
    (SixdayBasicAndPlus, Voucher) -> BigDecimal(779.88),
    (WeekendBasicAndPlus, Voucher) -> BigDecimal(359.88),
    (SaturdayBasicAndPlus, Voucher) -> BigDecimal(203.88),
    (EverydayBasicAndPlus, HomeDelivery) -> BigDecimal(1067.88),
    (SixdayBasicAndPlus, HomeDelivery) -> BigDecimal(935.88),
    (WeekendBasicAndPlus, HomeDelivery) -> BigDecimal(443.88),
    (SaturdayBasicAndPlus, HomeDelivery) -> BigDecimal(263.88),
  )

  def getNewPrice(billingPeriod: BillingPeriod, fullfilment: NxFulfillment, pack: NxPackage): Option[BigDecimal] = {
    billingPeriod match {
      case Monthly    => priceGridNewPricesMonthlies.get(pack, fullfilment)
      case Quarterly  => priceGridNewPricesQuarterlies.get(pack, fullfilment)
      case SemiAnnual => priceGridNewPricesSemiannuals.get(pack, fullfilment)
      case Annual     => priceGridNewPricesAnnuals.get(pack, fullfilment)
    }
  }

  def productNameToFullfilment(productName: String): Option[NxFulfillment] = {
    productName match {
      case "Newspaper Voucher"             => Some(Voucher)
      case "Newspaper Digital Voucher"     => Some(Voucher)
      case "Newspaper Delivery"            => Some(HomeDelivery)
      case "Newspaper - National Delivery" => Some(HomeDelivery)
      case _                               => None
    }
  }

  def decideFulfillment(subscription: ZuoraSubscription, today: LocalDate): Option[NxFulfillment] = {
    for {
      ratePlan <- SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription, today)
      fulfillment <- productNameToFullfilment(ratePlan.productName)
    } yield fulfillment
  }

  def ratePlanNameToPackage(rpn: String): Option[NxPackage] = {
    rpn match {
      case "Everyday"    => Some(EverydayBasicAndPlus)
      case "Everyday+"   => Some(EverydayBasicAndPlus)
      case "Sixday"      => Some(SixdayBasicAndPlus)
      case "Sixday+"     => Some(SixdayBasicAndPlus)
      case "Weekend"     => Some(WeekendBasicAndPlus)
      case "Weekend+"    => Some(WeekendBasicAndPlus)
      case "Saturday"    => Some(SaturdayBasicAndPlus)
      case "Saturday+"   => Some(SaturdayBasicAndPlus)
      case "Echo-Legacy" => Some(EchoLegacy)
      case _             => None
    }
  }

  def decidePackage(subscription: ZuoraSubscription, today: LocalDate): Option[NxPackage] = {
    for {
      ratePlan <- SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription, today)
      pack <- ratePlanNameToPackage(ratePlan.ratePlanName)
    } yield pack
  }

  def decideBrandTitle(subscription: ZuoraSubscription, today: LocalDate): Option[String] = {
    for {
      pack <- decidePackage(subscription, today)
    } yield {
      pack match {
        case EverydayBasicAndPlus => "the Guardian and the Observer"
        case SixdayBasicAndPlus   => "the Guardian"
        case WeekendBasicAndPlus  => "the Guardian and the Observer"
        case SaturdayBasicAndPlus => "the Guardian"
        case EchoLegacy           =>
          "the Guardian" // later I will double check if any of them has a Sunday delivery or not, and if any, will use a look up
      }
    }
  }

  def decideBranchTitleForNotificationHandler(
      cohortSpec: CohortSpec,
      subscription: ZuoraSubscription,
      today: LocalDate
  ): Option[String] = {
    MigrationType(cohortSpec) match {
      case Test1                         => Some("")
      case GuardianWeekly2025            => Some("")
      case Newspaper2025P1               => Some("")
      case Newspaper2025P3               => Some("")
      case ProductMigration2025N4        => Some("")
      case Membership2025                => Some("")
      case DigiSubs2025                  => Some("")
      case SupporterPlus2026             => Some("")
      case SupporterPlus2026N2           => Some("")
      case SupporterPlus2026N3           => Some("")
      case SupporterPlus2026N4           => Some("")
      case SupporterPlus2026N5           => Some("")
      case Print2026C1GWAnnualsUK        => Some("")
      case Print2026C1GWQuarterliesUK    => Some("")
      case Print2026C1NPAnnualsUK        => decideBrandTitle(subscription, today)
      case Print2026C1NPQuarterliesUK    => decideBrandTitle(subscription, today)
      case Print2026C1NPSemiannualsUK    => decideBrandTitle(subscription, today)
      case Print2026C2NPMonthliesUK      => decideBrandTitle(subscription, today)
      case Print2026C3GWMonthliesUK      => Some("")
      case Print2026C3NPMonthliesUK      => decideBrandTitle(subscription, today)
      case Print2026C4NPMonthliesUK      => decideBrandTitle(subscription, today)
      case Print2026C5GW                 => Some("")
      case Print2026C5NP                 => decideBrandTitle(subscription, today)
      case Print2026C6GWQuarterliesNonUK => Some("")
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

  def priceDataStandardNewspaper(
      cohortSpec: CohortSpec,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      account: ZuoraAccount,
      today: LocalDate
  ): Either[DataExtractionFailure, PriceData] = {
    val priceDataOpt: Option[PriceData] = for {
      ratePlan <- SI2025RateplanFromSubAndInvoices
        .determineRatePlan(subscription, invoiceList)
        .map(logValue("ratePlan"))
      currency <- SI2025Extractions.determineCurrency(ratePlan).map(logValue("currency"))
      oldPrice = logValue("oldPrice")(SI2025Extractions.determineOldPrice(ratePlan))
      billingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan).map(logValue("billingPeriod"))
      fullfilment <- decideFulfillment(subscription, today).map(logValue("fullfilment"))
      pack <- decidePackage(subscription, today).map(logValue("pack"))
      newPrice <- getNewPrice(billingPeriod, fullfilment, pack).map(logValue("newPrice"))
      commsPrice = logValue("commsPrice")(EstimationHandlerHelper.commsPrice(cohortSpec, oldPrice, newPrice))
    } yield PriceData(currency, oldPrice, newPrice, commsPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None            =>
        Left(
          DataExtractionFailure(
            s"[a149987a] Could not determine PriceData for subscription ${subscription.subscriptionNumber}"
          )
        )
    }
  }

  def priceDataEchoLegacy(
      cohortSpec: CohortSpec,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      account: ZuoraAccount,
      today: LocalDate
  ): Either[DataExtractionFailure, PriceData] = {
    val priceDataOpt: Option[PriceData] = for {
      ratePlan <- SI2025RateplanFromSubAndInvoices
        .determineRatePlan(subscription, invoiceList)
        .map(logValue("ratePlan"))
      currency <- SI2025Extractions.determineCurrency(ratePlan).map(logValue("currency"))
      oldPrice = logValue("oldPrice")(SI2025Extractions.determineOldPrice(ratePlan))
      billingPeriod <- SI2025Extractions.determineBillingPeriod(ratePlan).map(logValue("billingPeriod"))
      newPrice = (oldPrice * 1.071).setScale(2, RoundingMode.DOWN) // we uniformly increase the old price by 7.1 %
      commsPrice = newPrice
    } yield PriceData(currency, oldPrice, newPrice, commsPrice, BillingPeriod.toString(billingPeriod))
    priceDataOpt match {
      case Some(pricedata) => Right(pricedata)
      case None            =>
        Left(
          DataExtractionFailure(
            s"[70e56095] Could not determine PriceData for subscription ${subscription.subscriptionNumber}"
          )
        )
    }
  }

  def priceData(
      cohortSpec: CohortSpec,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      account: ZuoraAccount,
      today: LocalDate
  ): Either[DataExtractionFailure, PriceData] = {
    decidePackage(subscription, today) match {
      case Some(pack) => {
        pack match {
          case EchoLegacy =>
            priceDataEchoLegacy(
              cohortSpec: CohortSpec,
              subscription: ZuoraSubscription,
              invoiceList: ZuoraInvoiceList,
              account: ZuoraAccount,
              today: LocalDate
            )
          case _ =>
            priceDataStandardNewspaper(
              cohortSpec: CohortSpec,
              subscription: ZuoraSubscription,
              invoiceList: ZuoraInvoiceList,
              account: ZuoraAccount,
              today: LocalDate
            )
        }
      }
      case None =>
        Left(
          DataExtractionFailure(
            s"[8be1f8eb] Could not determine NxPackage for subscription ${subscription.subscriptionNumber}"
          )
        )
    }
  }

  def amendmentOrderPayload(
      cohortItem: CohortItem,
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      zuora_subscription: ZuoraSubscription,
      oldPrice: BigDecimal,
      commsPrice: BigDecimal,
      invoiceList: ZuoraInvoiceList,
  ): Either[Failure, Value] = {
    // This version of `amendmentOrderPayload`, applied to subscriptions with the active rate plan having
    // several charges (one per delivery day), is using ZuoraOrdersApiPrimitives.ratePlanChargesToChargeOverrides
    // which maps the rate plan's rate plan charges to an array of charge overrides json objects.

    // The important preliminary here, which wasn't needed in the simpler case of a single rate plan charge
    // in the case of GuardianWeekly2025, for instance, is the price ratio from the old price to the new price
    // (both carried by the cohort item).

    val priceRatio = commsPrice / oldPrice

    val order_opt = for {
      ratePlan <- SI2025RateplanFromSubAndInvoices.determineRatePlan(zuora_subscription, invoiceList)
      billingPeriod <- ZuoraRatePlan.ratePlanToOptionalUniquelyDeterminedBillingPeriod(ratePlan)
    } yield {
      val subscriptionRatePlanId = ratePlan.id
      val removeProduct = ZuoraOrdersApiPrimitives.removeProduct(effectDate.toString, subscriptionRatePlanId)
      val triggerDateString = effectDate.toString
      val productRatePlanId = ratePlan.productRatePlanId // We are upgrading on the same rate plan.
      val chargeOverrides: List[Value] = ZuoraOrdersApiPrimitives.ratePlanChargesToChargeOverrides(
        ratePlan.ratePlanCharges,
        priceRatio,
        commsPrice,
        BillingPeriod.toString(billingPeriod)
      )
      val addProduct = ZuoraOrdersApiPrimitives.addProduct(triggerDateString, productRatePlanId, chargeOverrides)
      val order_subscription =
        ZuoraOrdersApiPrimitives.subscription(subscriptionNumber, List(removeProduct), List(addProduct))
      ZuoraOrdersApiPrimitives.subscriptionUpdatePayload(
        orderDate.toString,
        accountNumber,
        order_subscription
      )
    }
    order_opt match {
      case Some(order) => Right(order)
      case None        =>
        Left(
          DataExtractionFailure(
            s"[9f480e70] Could not compute amendmentOrderPayload for subscription ${zuora_subscription.subscriptionNumber}"
          )
        )
    }
  }
}
