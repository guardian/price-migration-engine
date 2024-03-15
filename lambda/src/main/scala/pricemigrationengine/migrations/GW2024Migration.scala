package pricemigrationengine.migrations

import pricemigrationengine.model._
import java.time.LocalDate

object GW2024Migration {

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

  def priceData(
      subscription: ZuoraSubscription,
      account: ZuoraAccount
  ): Either[AmendmentDataFailure, PriceData] = {
    ???
  }

  def zuoraUpdate(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {
    ???
  }
}
