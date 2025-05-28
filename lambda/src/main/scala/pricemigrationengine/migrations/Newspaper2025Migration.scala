package pricemigrationengine.migrations
import pricemigrationengine.model.PriceCap
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._
import pricemigrationengine.libs._

import java.time.LocalDate

object Newspaper2025Migration {

  val maxLeadTime = 39
  val minLeadTime = 37

  // ------------------------------------------------
  // Primary Functions:
  //
  // The primary functions are the main functions that
  // are implemented by the *Migration module.
  //
  // priceData is used in the Estimation handler
  // amendmentOrderPayload is used in the Amendment handler
  // ------------------------------------------------

  def priceData(
      subscription: ZuoraSubscription
  ): Either[Failure, PriceData] = {
    ???
  }

  def amendmentOrderPayload(
      orderDate: LocalDate,
      accountNumber: String,
      subscriptionNumber: String,
      effectDate: LocalDate,
      subscription: ZuoraSubscription,
      oldPrice: BigDecimal,
      estimatedNewPrice: BigDecimal,
      priceCap: BigDecimal
  ): Either[Failure, ZuoraAmendmentOrderPayload] = {
    ???
  }
}
