package pricemigrationengine.model

import java.time.LocalDate

case class AmendmentData(startDate: LocalDate, newPrice: Double)

object AmendmentData {

  def apply(
      subscription: ZuoraSubscription,
      account: ZuoraAccount,
      earliestStartDate: LocalDate,
      currentDate: LocalDate
  ): Either[AmendmentDataFailure, AmendmentData] =
    for {
      startDate <- BillingDate.nextBillingDate(
        subscription,
        account,
        after = earliestStartDate.minusDays(1),
        currentDate
      )
      price <- newPrice(subscription)
    } yield AmendmentData(startDate, price)

  // TODO
  def newPrice(subscription: ZuoraSubscription): Either[AmendmentDataFailure, Double] =
    Left(AmendmentDataFailure("newPrice not implemented!"))
}
