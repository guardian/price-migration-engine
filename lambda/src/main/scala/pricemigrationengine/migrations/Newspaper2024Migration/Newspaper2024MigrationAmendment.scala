package pricemigrationengine.migrations
import pricemigrationengine.migrations.Newspaper2024MigrationStaticData._
import pricemigrationengine.model._

import java.time.LocalDate

object Newspaper2024MigrationAmendment {

  def subscriptionToNewPriceDistribution(subscription: ZuoraSubscription): Option[RatePlanCharges2024] = {
    for {
      productName <- Newspaper2024MigrationEstimation.subscriptionToMigrationProductName(subscription).toOption
      ratePlanDetails <- Newspaper2024MigrationEstimation
        .subscriptionToRatePlanDetails(subscription, productName)
        .toOption
      priceDistribution <- Newspaper2024MigrationStaticData.priceDistributionLookup(
        productName,
        ratePlanDetails.billingPeriod,
        ratePlanDetails.ratePlanName
      )
    } yield priceDistribution
  }

  def priceDistributionToChargeOverrides(
      distribution: RatePlanCharges2024,
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
    ).flatten.map { price =>
      ChargeOverride(
        productRatePlanChargeId = "ratePlanChargeId",
        billingPeriod = billingPeriod,
        price = price
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
