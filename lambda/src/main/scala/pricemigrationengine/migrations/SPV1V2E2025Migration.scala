package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._

import java.time.LocalDate

object SPV1V2E2025Migration {
  val maxLeadTime = 35
  val minLeadTime = 32

  def priceData(subscription: ZuoraSubscription): Either[Failure, PriceData] = ???

  // This need to be implemented before the end of Feb 2025
  def zuoraUpdate(
      subscriptionBeforeUpdate: ZuoraSubscription,
      startDate: LocalDate
  ): Either[DataExtractionFailure, ZuoraSubscriptionUpdate] = ???

}
