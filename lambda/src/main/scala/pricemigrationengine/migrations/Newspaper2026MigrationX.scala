package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}

import java.time.LocalDate
import ujson._

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
      case "Newspaper Voucher"         => Some(Voucher)
      case "Newspaper Digital Voucher" => Some(Voucher)
      case "Newspaper Delivery"        => Some(HomeDelivery)
      case _                           => None
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
      case "Everyday"  => Some(EverydayBasicAndPlus)
      case "Everyday+" => Some(EverydayBasicAndPlus)
      case "Sixday"    => Some(SixdayBasicAndPlus)
      case "Sixday+"   => Some(SixdayBasicAndPlus)
      case "Weekend"   => Some(WeekendBasicAndPlus)
      case "Weekend+"  => Some(WeekendBasicAndPlus)
      case "Saturday"  => Some(SaturdayBasicAndPlus)
      case "Saturday+" => Some(SaturdayBasicAndPlus)
      case _           => None
    }
  }

  def decidePackage(subscription: ZuoraSubscription, today: LocalDate): Option[NxPackage] = {
    for {
      ratePlan <- SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription, today)
      pack <- ratePlanNameToPackage(ratePlan.ratePlanName)
    } yield pack
  }
}
