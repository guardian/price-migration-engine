package pricemigrationengine.model

import upickle.default.{ReadWriter, macroRW}

case class ZuoraInvoiceList(invoiceItems: Seq[ZuoraInvoiceItem])

object ZuoraInvoiceList {
  implicit val rw: ReadWriter[ZuoraInvoiceList] = macroRW
}
