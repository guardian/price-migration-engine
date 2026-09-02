package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}

import java.time.LocalDate
import ujson._
import upickle.default._

import java.time.format.DateTimeFormatter

sealed trait NxFullfilment
object VoucherCard extends NxFullfilment
object HomeDelivery extends NxFullfilment

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

  val priceGridNewPricesMonthlies: Map[(NxFullfilment, NxPackage), BigDecimal] = Map(
    (VoucherCard, EverydayBasicAndPlus) -> BigDecimal(72.99),
    (VoucherCard, SixdayBasicAndPlus) -> BigDecimal(64.99),
    (VoucherCard, WeekendBasicAndPlus) -> BigDecimal(29.99),
    (VoucherCard, SaturdayBasicAndPlus) -> BigDecimal(16.99),
    (HomeDelivery, EverydayBasicAndPlus) -> BigDecimal(88.99),
    (HomeDelivery, SixdayBasicAndPlus) -> BigDecimal(77.99),
    (HomeDelivery, WeekendBasicAndPlus) -> BigDecimal(36.99),
    (HomeDelivery, SaturdayBasicAndPlus) -> BigDecimal(21.99),
  )
  val priceGridNewPricesQuarterlies: Map[(NxFullfilment, NxPackage), BigDecimal] = Map(
    (VoucherCard, EverydayBasicAndPlus) -> BigDecimal(218.97),
    (VoucherCard, SixdayBasicAndPlus) -> BigDecimal(194.97),
    (VoucherCard, WeekendBasicAndPlus) -> BigDecimal(89.97),
    (VoucherCard, SaturdayBasicAndPlus) -> BigDecimal(50.97),
    (HomeDelivery, EverydayBasicAndPlus) -> BigDecimal(266.97),
    (HomeDelivery, SixdayBasicAndPlus) -> BigDecimal(233.97),
    (HomeDelivery, WeekendBasicAndPlus) -> BigDecimal(110.97),
    (HomeDelivery, SaturdayBasicAndPlus) -> BigDecimal(65.97),
  )
  val priceGridNewPricesSemiannuals: Map[(NxFullfilment, NxPackage), BigDecimal] = Map(
    (VoucherCard, EverydayBasicAndPlus) -> BigDecimal(437.94),
    (VoucherCard, SixdayBasicAndPlus) -> BigDecimal(389.94),
    (VoucherCard, WeekendBasicAndPlus) -> BigDecimal(179.9),
    (VoucherCard, SaturdayBasicAndPlus) -> BigDecimal(101.94),
    (HomeDelivery, EverydayBasicAndPlus) -> BigDecimal(533.94),
    (HomeDelivery, SixdayBasicAndPlus) -> BigDecimal(467.94),
    (HomeDelivery, WeekendBasicAndPlus) -> BigDecimal(221.94),
    (HomeDelivery, SaturdayBasicAndPlus) -> BigDecimal(131.94),
  )
  val priceGridNewPricesAnnuals: Map[(NxFullfilment, NxPackage), BigDecimal] = Map(
    (VoucherCard, EverydayBasicAndPlus) -> BigDecimal(875.88),
    (VoucherCard, SixdayBasicAndPlus) -> BigDecimal(779.88),
    (VoucherCard, WeekendBasicAndPlus) -> BigDecimal(359.88),
    (VoucherCard, SaturdayBasicAndPlus) -> BigDecimal(203.88),
    (HomeDelivery, EverydayBasicAndPlus) -> BigDecimal(1067.88),
    (HomeDelivery, SixdayBasicAndPlus) -> BigDecimal(935.88),
    (HomeDelivery, WeekendBasicAndPlus) -> BigDecimal(443.88),
    (HomeDelivery, SaturdayBasicAndPlus) -> BigDecimal(263.88),
  )

  def getNewPrice(billingPeriod: BillingPeriod, fullfilment: NxFullfilment, pack: NxPackage): Option[BigDecimal] = {
    billingPeriod match {
      case Monthly    => priceGridNewPricesMonthlies.get(fullfilment, pack)
      case Quarterly  => priceGridNewPricesQuarterlies.get(fullfilment, pack)
      case SemiAnnual => priceGridNewPricesSemiannuals.get(fullfilment, pack)
      case Annual     => None
    }
  }
}
