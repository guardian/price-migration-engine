package pricemigrationengine.model

import java.time.LocalDate

case class AmendmentData(startDate: LocalDate, newPrice: Double)

object AmendmentData {

  def apply(
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList,
      earliestStartDate: LocalDate
  ): Either[AmendmentDataFailure, AmendmentData] =
    for {
      startDate <- nextBillingDate(invoiceList, after = earliestStartDate.minusDays(1))
      price <- newPrice(subscription)
    } yield AmendmentData(startDate, newPrice = price)

  def nextBillingDate(invoiceList: ZuoraInvoiceList, after: LocalDate): Either[AmendmentDataFailure, LocalDate] = {
    invoiceList.invoiceItems
      .map(_.serviceStartDate)
      .sortBy(_.toEpochDay)
      .dropWhile(date => !date.isAfter(after))
      .headOption
      .toRight(AmendmentDataFailure(s"Cannot determine next billing date after $after from $invoiceList"))
  }

  // TODO
  def newPrice(subscription: ZuoraSubscription): Either[AmendmentDataFailure, Double] =
    Left(AmendmentDataFailure("newPrice not implemented!"))
}
