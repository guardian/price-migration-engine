package pricemigrationengine.model

import java.time.LocalDate

import upickle.default.{ReadWriter, macroRW}

case class ZuoraRatePlanCharge(
    name: String,
    price: Double,
    billingPeriod: Option[String] = None,
    effectiveStartDate: LocalDate,
    chargedThroughDate: Option[LocalDate] = None,
    processedThroughDate: Option[LocalDate] = None,
    specificBillingPeriod: Option[Int] = None,
    endDateCondition: Option[String] = None,
    upToPeriodsType: Option[String] = None,
    upToPeriods: Option[Int] = None,
    billingDay: Option[String] = None,
    triggerEvent: Option[String] = None,
    triggerDate: Option[LocalDate] = None,
    discountPercentage: Option[Double] = None
)

object ZuoraRatePlanCharge {
  implicit val rw: ReadWriter[ZuoraRatePlanCharge] = macroRW
}
