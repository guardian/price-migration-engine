package pricemigrationengine.model

import java.time.LocalDate

import upickle.default.{ReadWriter, macroRW}

case class ZuoraInvoiceList(invoiceItems: Seq[ZuoraInvoiceItem])

object ZuoraInvoiceList {
  implicit val rw: ReadWriter[ZuoraInvoiceList] = macroRW
}

/*
 * This is only useful for finding which rate plan charges apply on a given date.
 *
 * Don't use 'chargeAmount' field as it doesn't include tax.
 * However the price field of the corresponding rate plan charge does include tax.
 */
case class ZuoraInvoiceItem(subscriptionNumber: String, serviceStartDate: LocalDate, chargeNumber: String)

object ZuoraInvoiceItem {
  implicit val rw: ReadWriter[ZuoraInvoiceItem] = macroRW

  def itemsForSubscription(
      invoiceList: ZuoraInvoiceList,
      subscription: ZuoraSubscription
  ): Seq[ZuoraInvoiceItem] =
    invoiceList.invoiceItems
      .filter(_.subscriptionNumber == subscription.subscriptionNumber)

  def items(
      invoiceList: ZuoraInvoiceList,
      subscription: ZuoraSubscription,
      serviceStartDate: LocalDate
  ): Seq[ZuoraInvoiceItem] =
    itemsForSubscription(invoiceList, subscription)
      .filter(_.serviceStartDate == serviceStartDate)
      .distinctBy(_.chargeNumber)
}
