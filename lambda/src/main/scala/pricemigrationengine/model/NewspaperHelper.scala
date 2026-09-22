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

  def ratePlanNameToT2xNewspaperPackage(ratePlanName: String): Either[String, T2xNewspaperPackage] = {
    ratePlanName match {
      case "Everyday"    => Right(T2xEveryday)
      case "Everyday+"   => Right(T2xEverydayPlus)
      case "Sixday"      => Right(T2xSixDay)
      case "Sixday+"     => Right(T2xSixDayPlus)
      case "Weekend"     => Right(T2xWeekend)
      case "Weekend+"    => Right(T2xWeekendPlus)
      case "Saturday"    => Right(T2xSaturday)
      case "Saturday+"   => Right(T2xSaturdayPlus)
      case "Echo-Legacy" => Right(T2xEchoLegacy)
      case _ => Left(s"[ecbe7602] Could not determine T2xNewspaperPackage for rate plan name: ${ratePlanName}")
    }
  }

  def subscriptionToT2xNewspaperPackage(
      subscription: ZuoraSubscription,
      today: LocalDate
  ): Either[String, T2xNewspaperPackage] = {
    for {
      ratePlan <- SI2025RateplanFromSub
        .uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription, today)
        .toRight(s"[9fe8471f] could not determine ratePlan for subscription: ${subscription.subscriptionNumber}")
      pack <- ratePlanNameToT2xNewspaperPackage(ratePlan.ratePlanName)
    } yield pack
  }
}
