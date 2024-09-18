package pricemigrationengine.model

import java.time.LocalDate

import pricemigrationengine.model.OptionReader // not sure why this import is needed as should be visible implicitly
import upickle.default._

case class ZuoraRatePlanCharge(
    productRatePlanChargeId: ZuoraProductRatePlanChargeId,
    name: String,
    number: String,
    currency: Currency,
    price: Option[BigDecimal] = None,
    billingPeriod: Option[String] = None,
    chargedThroughDate: Option[LocalDate] = None,
    processedThroughDate: Option[LocalDate] = None,
    specificBillingPeriod: Option[Int] = None,
    endDateCondition: Option[String] = None,
    upToPeriodsType: Option[String] = None,
    upToPeriods: Option[Int] = None,
    billingDay: Option[String] = None,
    triggerEvent: Option[String] = None,
    triggerDate: Option[LocalDate] = None,
    discountPercentage: Option[Double] = None,
    originalOrderDate: Option[LocalDate] = None,
    effectiveStartDate: Option[LocalDate] = None,
    effectiveEndDate: Option[LocalDate] = None,
)

object ZuoraRatePlanCharge {
  implicit val rw: ReadWriter[ZuoraRatePlanCharge] = macroRW

  /** Rate plan charge that corresponds with the given invoice item.
    */
  def matchingRatePlanCharge(
      subscription: ZuoraSubscription,
      invoiceItem: ZuoraInvoiceItem
  ): Either[DataExtractionFailure, ZuoraRatePlanCharge] =
    (for {
      ratePlan <- subscription.ratePlans
      ratePlanCharge <- ratePlan.ratePlanCharges
      if ratePlanCharge.number == invoiceItem.chargeNumber
    } yield ratePlanCharge).headOption
      .toRight(DataExtractionFailure(s"No matching rate plan charge for invoice item '${invoiceItem.chargeNumber}'"))
}
