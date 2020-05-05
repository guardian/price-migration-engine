package pricemigrationengine.model

import java.time.LocalDate

import upickle.default.{ReadWriter, macroRW}

case class ZuoraInvoiceItem(serviceStartDate: LocalDate)

object ZuoraInvoiceItem {
  implicit val rw: ReadWriter[ZuoraInvoiceItem] = macroRW
}
