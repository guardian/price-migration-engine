package pricemigrationengine.model

import java.time.LocalDate

import upickle.default._

case class ZuoraSubscriptionUpdate(
    add: Seq[AddZuoraRatePlan],
    remove: Seq[RemoveZuoraRatePlan]
)

object ZuoraSubscriptionUpdate {
  implicit val rw: ReadWriter[ZuoraSubscriptionUpdate] = macroRW

  /**
    * Takes all non-discount rate plans participating in the invoice list on the given date,
    * and replaces them with their current equivalent.
    * This has the effect of updating their prices to the current ones in the product catalogue.
    */
  def updateOfRatePlansToCurrent(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      date: LocalDate
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    val ratePlans = (for {
      invoiceItem <- ZuoraInvoiceItem.items(invoiceList, subscription, date)
      ratePlanCharge <- ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toSeq
      price <- ratePlanCharge.price.toSeq
      if price > 0
      ratePlan <- ZuoraRatePlan.ratePlan(subscription, ratePlanCharge).toSeq
    } yield ratePlan).distinct

    if (ratePlans.isEmpty)
      Left(AmendmentDataFailure("No rate plans to update"))
    else
      Right(
        ZuoraSubscriptionUpdate(
          add = ratePlans.map(ratePlan => AddZuoraRatePlan(ratePlan.productRatePlanId, date)),
          remove = ratePlans.map(ratePlan => RemoveZuoraRatePlan(ratePlan.id, date))
        )
      )
  }
}

case class AddZuoraRatePlan(
    productRatePlanId: String,
    contractEffectiveDate: LocalDate
)

object AddZuoraRatePlan {
  implicit val rw: ReadWriter[AddZuoraRatePlan] = macroRW
}

case class RemoveZuoraRatePlan(
    ratePlanId: String,
    contractEffectiveDate: LocalDate
)

object RemoveZuoraRatePlan {
  implicit val rw: ReadWriter[RemoveZuoraRatePlan] = macroRW
}
