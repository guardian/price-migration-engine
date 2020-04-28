package pricemigrationengine.model

import java.time.LocalDate

case class AmendmentData(startDate: LocalDate, newPrice: Double)

object AmendmentData {

  def apply(
      subscription: ZuoraSubscription,
      earliestStartDate: LocalDate
  ): Either[AmendmentDataFailure, AmendmentData] =
    for {
      startDate <- nextBillingDate(subscription, earliestStartDate.minusDays(1))
      price <- newPrice(subscription)
    } yield AmendmentData(startDate, price)

  // TODO
  def nextBillingDate(subscription: ZuoraSubscription, after: LocalDate): Either[AmendmentDataFailure, LocalDate] =
    Left(AmendmentDataFailure("nextBillingDate not implemented!"))

  // TODO
  def newPrice(subscription: ZuoraSubscription): Either[AmendmentDataFailure, Double] =
    Left(AmendmentDataFailure("newPrice not implemented!"))
}
