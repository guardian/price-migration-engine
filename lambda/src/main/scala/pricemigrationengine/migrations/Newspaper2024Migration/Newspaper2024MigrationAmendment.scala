package pricemigrationengine.migrations
import pricemigrationengine.migrations.Newspaper2024MigrationStaticData._
import pricemigrationengine.model._

import java.time.LocalDate

object Newspaper2024MigrationAmendment {

  def subscriptionToNewPriceDistribution(subscription: ZuoraSubscription): Option[ChargeDistribution2024] = {
    for {
      data2024 <- Newspaper2024MigrationEstimation
        .subscriptionToSubscriptionData2024(subscription)
        .toOption
      priceDistribution <- Newspaper2024MigrationStaticData.priceDistributionLookup(
        data2024.productName,
        data2024.billingPeriod,
        data2024.ratePlanName
      )
    } yield priceDistribution
  }

  def priceDistributionToChargeOverrides(
      distribution: ChargeDistribution2024,
      billingPeriod: String
  ): List[ChargeOverride] = {
    List(
      distribution.monday,
      distribution.tuesday,
      distribution.wednesday,
      distribution.thursday,
      distribution.friday,
      distribution.saturday,
      distribution.sunday,
      distribution.digitalPack,
    ).flatten.map { individualCharge =>
      ChargeOverride(
        productRatePlanChargeId = individualCharge.chargeId,
        billingPeriod = billingPeriod,
        price = individualCharge.Price
      )
    }
  }

  def subscriptionToZuoraSubscriptionUpdate(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    val product = "Newspaper Delivery"
    val ratePlanName = "EveryDay"
    val ratePlanId = Newspaper2024MigrationStaticData.ratePlanIdLookUp(product, ratePlanName).get
    val distribution = subscriptionToNewPriceDistribution(subscription).get
    val billingPeriod = "Month"

    Right(
      ZuoraSubscriptionUpdate(
        add = List(
          AddZuoraRatePlan(
            productRatePlanId = ratePlanId,
            contractEffectiveDate = effectiveDate,
            chargeOverrides = priceDistributionToChargeOverrides(distribution, billingPeriod)
          )
        ),
        remove = List(
          RemoveZuoraRatePlan(
            ratePlanId = ratePlanId,
            effectiveDate
          )
        ),
        currentTerm = None,
        currentTermPeriodType = None
      )
    )
  }
}
