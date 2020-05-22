package pricemigrationengine.model

import java.time.LocalDate

import upickle.default.{ReadWriter, macroRW}

case class ZuoraInvoiceList(invoiceItems: Seq[ZuoraInvoiceItem])

object ZuoraInvoiceList {
  implicit val rw: ReadWriter[ZuoraInvoiceList] = macroRW
}

case class ZuoraInvoiceItem(serviceStartDate: LocalDate, chargeAmount: BigDecimal, chargeNumber: String)

object ZuoraInvoiceItem {
  implicit val rw: ReadWriter[ZuoraInvoiceItem] = macroRW

  def items(invoiceList: ZuoraInvoiceList, serviceStartDate: LocalDate): Seq[ZuoraInvoiceItem] =
    invoiceList.invoiceItems.filter(_.serviceStartDate == serviceStartDate)
}
