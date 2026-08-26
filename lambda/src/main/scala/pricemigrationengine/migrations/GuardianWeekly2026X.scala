package pricemigrationengine.migrations
import pricemigrationengine.model.{BillingPeriod, ZuoraRatePlan, _}

import java.time.LocalDate
import ujson._
import upickle.default._

import java.time.format.DateTimeFormatter

object GuardianWeekly2026X {
  type Currency = String
  type Region = String

  val priceGridNewPricesMonthlies: Map[(Currency, Region), BigDecimal] = Map(
    ("GBP", "UK") -> BigDecimal(17.50),
    ("EURO", "EU") -> BigDecimal(30.50),
    ("USD", "ROW") -> BigDecimal(38.00),
    ("USD", "US") -> BigDecimal(33.00),
    ("CAD", "CAN") -> BigDecimal(39.50),
    ("AUD", "AU") -> BigDecimal(48.00),
    ("NZD", "NZ") -> BigDecimal(60.00),
  )

  val priceGridNewPricesQuarterlies: Map[(Currency, Region), BigDecimal] = Map(
    ("GBP", "UK") -> BigDecimal(52),
    ("EURO", "EU") -> BigDecimal(91.5),
    ("USD", "ROW") -> BigDecimal(114),
    ("USD", "US") -> BigDecimal(99),
    ("CAD", "CAN") -> BigDecimal(118.5),
    ("AUD", "AU") -> BigDecimal(144),
    ("NZD", "NZ") -> BigDecimal(180),
  )

  val priceGridNewPricesAnnuals: Map[(Currency, Region), BigDecimal] = Map(
    ("GBP", "UK") -> BigDecimal(208),
    ("EURO", "EU") -> BigDecimal(366),
    ("USD", "ROW") -> BigDecimal(456),
    ("USD", "US") -> BigDecimal(396),
    ("CAD", "CAN") -> BigDecimal(474.0),
    ("AUD", "AU") -> BigDecimal(576),
    ("NZD", "NZ") -> BigDecimal(720),
  )

  def getNewPrice(billingPeriod: BillingPeriod, currency: Currency, region: Region): Option[BigDecimal] = {
    billingPeriod match {
      case Monthly    => priceGridNewPricesMonthlies.get(currency, region)
      case Quarterly  => priceGridNewPricesQuarterlies.get(currency, region)
      case SemiAnnual => None
      case Annual     => priceGridNewPricesAnnuals.get(currency, region)
    }
  }
}
