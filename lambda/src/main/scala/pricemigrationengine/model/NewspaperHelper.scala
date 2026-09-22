package pricemigrationengine.model

import java.time.LocalDate

object NewspaperHelper {
  // NewspaperHelper was introduced in September 2026 alongside the
  // NewspaperHelperLegPercentageDistribution financial data.

  def productNameToT3xDeliveryCategory(productName: String): Option[T3xDeliveryCategory] = {
    productName match {
      case "Newspaper Voucher"             => Some(T3xNewspaperVoucher)
      case "Newspaper Digital Voucher"     => Some(T3xNewspaperDigitalVoucher)
      case "Newspaper Delivery"            => Some(T3xNewspaperDelivery)
      case "Newspaper - National Delivery" => Some(T3xNewspaperNationalDelivery)
      case _                               => None
    }
  }

  def subscriptionToT3xDeliveryCategory(subscription: ZuoraSubscription, today: LocalDate) = {
    for {
      ratePlan <- SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription, today)
      fulfillment <- productNameToT3xDeliveryCategory(ratePlan.productName)
    } yield fulfillment
  }
}
