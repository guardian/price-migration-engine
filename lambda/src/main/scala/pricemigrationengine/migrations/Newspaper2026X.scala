package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}

import java.time.LocalDate
import ujson._
import upickle.default._

import java.time.format.DateTimeFormatter

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

object Newspaper2026X {

  val priceGridNewPricesMonthlies: Map[(NxFulfillment, NxPackage), BigDecimal] = Map(
    (Voucher, EverydayBasicAndPlus) -> BigDecimal(72.99),
    (Voucher, SixdayBasicAndPlus) -> BigDecimal(64.99),
    (Voucher, WeekendBasicAndPlus) -> BigDecimal(29.99),
    (Voucher, SaturdayBasicAndPlus) -> BigDecimal(16.99),
    (HomeDelivery, EverydayBasicAndPlus) -> BigDecimal(88.99),
    (HomeDelivery, SixdayBasicAndPlus) -> BigDecimal(77.99),
    (HomeDelivery, WeekendBasicAndPlus) -> BigDecimal(36.99),
    (HomeDelivery, SaturdayBasicAndPlus) -> BigDecimal(21.99),
  )
  val priceGridNewPricesQuarterlies: Map[(NxFulfillment, NxPackage), BigDecimal] = Map(
    (Voucher, EverydayBasicAndPlus) -> BigDecimal(218.97),
    (Voucher, SixdayBasicAndPlus) -> BigDecimal(194.97),
    (Voucher, WeekendBasicAndPlus) -> BigDecimal(89.97),
    (Voucher, SaturdayBasicAndPlus) -> BigDecimal(50.97),
    (HomeDelivery, EverydayBasicAndPlus) -> BigDecimal(266.97),
    (HomeDelivery, SixdayBasicAndPlus) -> BigDecimal(233.97),
    (HomeDelivery, WeekendBasicAndPlus) -> BigDecimal(110.97),
    (HomeDelivery, SaturdayBasicAndPlus) -> BigDecimal(65.97),
  )
  val priceGridNewPricesSemiannuals: Map[(NxFulfillment, NxPackage), BigDecimal] = Map(
    (Voucher, EverydayBasicAndPlus) -> BigDecimal(437.94),
    (Voucher, SixdayBasicAndPlus) -> BigDecimal(389.94),
    (Voucher, WeekendBasicAndPlus) -> BigDecimal(179.9),
    (Voucher, SaturdayBasicAndPlus) -> BigDecimal(101.94),
    (HomeDelivery, EverydayBasicAndPlus) -> BigDecimal(533.94),
    (HomeDelivery, SixdayBasicAndPlus) -> BigDecimal(467.94),
    (HomeDelivery, WeekendBasicAndPlus) -> BigDecimal(221.94),
    (HomeDelivery, SaturdayBasicAndPlus) -> BigDecimal(131.94),
  )
  val priceGridNewPricesAnnuals: Map[(NxFulfillment, NxPackage), BigDecimal] = Map(
    (Voucher, EverydayBasicAndPlus) -> BigDecimal(875.88),
    (Voucher, SixdayBasicAndPlus) -> BigDecimal(779.88),
    (Voucher, WeekendBasicAndPlus) -> BigDecimal(359.88),
    (Voucher, SaturdayBasicAndPlus) -> BigDecimal(203.88),
    (HomeDelivery, EverydayBasicAndPlus) -> BigDecimal(1067.88),
    (HomeDelivery, SixdayBasicAndPlus) -> BigDecimal(935.88),
    (HomeDelivery, WeekendBasicAndPlus) -> BigDecimal(443.88),
    (HomeDelivery, SaturdayBasicAndPlus) -> BigDecimal(263.88),
  )

  def getNewPrice(billingPeriod: BillingPeriod, fullfilment: NxFulfillment, pack: NxPackage): Option[BigDecimal] = {
    billingPeriod match {
      case Monthly    => priceGridNewPricesMonthlies.get(fullfilment, pack)
      case Quarterly  => priceGridNewPricesQuarterlies.get(fullfilment, pack)
      case SemiAnnual => priceGridNewPricesSemiannuals.get(fullfilment, pack)
      case Annual     => None
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

  // sub2: "Newspaper Digital Voucher"  "Everyday+"
  // sub3: "Newspaper Delivery"         "Everyday+"
  // sub4: "Newspaper Voucher"          "Sixday+"
  // sub5: "Newspaper Voucher"          "Weekend+"    "GBP"   "Month"
  // sub6: "Newspaper Voucher"          "Everyday"
  // sub7: "Newspaper Voucher"          "Sixday"
  // sub8: "Newspaper Voucher"          "Sixday+"     "GBP"   "Quarter"
  // sub9: "Newspaper Voucher"          "Everyday+"   "GBP"   "Annual"

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
