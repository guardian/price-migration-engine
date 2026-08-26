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
object EverydayPlus extends NxPackage
object SixdayPlus extends NxPackage
object WeekendPlus extends NxPackage
object SaturdayPlus extends NxPackage

object Newspaper2026X {

  val priceGridNewPricesMonthlies: Map[(NxFullfilment, NxPackage), BigDecimal] = Map(
    (VoucherCard, EverydayPlus) -> BigDecimal(72.99),
    (VoucherCard, SixdayPlus) -> BigDecimal(64.99),
    (VoucherCard, WeekendPlus) -> BigDecimal(29.99),
    (VoucherCard, SaturdayPlus) -> BigDecimal(16.99),
    (HomeDelivery, EverydayPlus) -> BigDecimal(88.99),
    (HomeDelivery, SixdayPlus) -> BigDecimal(77.99),
    (HomeDelivery, WeekendPlus) -> BigDecimal(36.99),
    (HomeDelivery, SaturdayPlus) -> BigDecimal(21.99),
  )
  val priceGridNewPricesQuarterlies: Map[(NxFullfilment, NxPackage), BigDecimal] = Map(
    (VoucherCard, EverydayPlus) -> BigDecimal(218.97),
    (VoucherCard, SixdayPlus) -> BigDecimal(194.97),
    (VoucherCard, WeekendPlus) -> BigDecimal(89.97),
    (VoucherCard, SaturdayPlus) -> BigDecimal(50.97),
    (HomeDelivery, EverydayPlus) -> BigDecimal(266.97),
    (HomeDelivery, SixdayPlus) -> BigDecimal(233.97),
    (HomeDelivery, WeekendPlus) -> BigDecimal(110.97),
    (HomeDelivery, SaturdayPlus) -> BigDecimal(65.97),
  )
  val priceGridNewPricesSemiannuals: Map[(NxFullfilment, NxPackage), BigDecimal] = Map(
    (VoucherCard, EverydayPlus) -> BigDecimal(437.94),
    (VoucherCard, SixdayPlus) -> BigDecimal(389.94),
    (VoucherCard, WeekendPlus) -> BigDecimal(179.9),
    (VoucherCard, SaturdayPlus) -> BigDecimal(101.94),
    (HomeDelivery, EverydayPlus) -> BigDecimal(533.94),
    (HomeDelivery, SixdayPlus) -> BigDecimal(467.94),
    (HomeDelivery, WeekendPlus) -> BigDecimal(221.94),
    (HomeDelivery, SaturdayPlus) -> BigDecimal(131.94),
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
