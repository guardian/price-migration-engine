package pricemigrationengine.model

import java.time.LocalDate

object NewspaperHelper {
  // NewspaperHelper was introduced in September 2026 alongside the
  // NewspaperHelperLegPercentageDistribution financial data.

  def productNameToT3xDeliveryCategory(productName: String): Either[String, T3xDeliveryCategory] = {
    productName match {
      case "Newspaper Voucher"             => Right(T3xNewspaperVoucher)
      case "Newspaper Digital Voucher"     => Right(T3xNewspaperDigitalVoucher)
      case "Newspaper Delivery"            => Right(T3xNewspaperDelivery)
      case "Newspaper - National Delivery" => Right(T3xNewspaperNationalDelivery)
      case _ => Left(s"[c79805c6] Could not determine T3xDeliveryCategory for product name: ${productName}")
    }
  }

  def subscriptionToT3xDeliveryCategory(
      subscription: ZuoraSubscription,
      today: LocalDate
  ): Either[String, T3xDeliveryCategory] = {
    for {
      ratePlan <- SI2025RateplanFromSub
        .uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription, today)
        .toRight(s"[294d16fc] could not determine ratePlan for subscription: ${subscription.subscriptionNumber}")
      fulfillment <- productNameToT3xDeliveryCategory(ratePlan.productName)
    } yield fulfillment
  }
}
