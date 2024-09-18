package pricemigrationengine.migrations.newspaper2024Migration
import pricemigrationengine.model._
import pricemigrationengine.migrations.newspaper2024Migration.StaticData._
import pricemigrationengine.migrations.newspaper2024Migration.Estimation._

import java.time.LocalDate

object Amendment {

  def subscriptionToNewChargeDistribution2024(subscription: ZuoraSubscription): Option[ChargeDistribution2024] = {
    val priceCorrectionFactor = PriceCapping.priceCorrectionFactor(subscription)
    for {
      data2024 <- Estimation
        .subscriptionToSubscriptionData2024(subscription)
        .toOption
      priceDistribution <- StaticData.priceDistributionLookup(
        data2024.productName,
        data2024.billingPeriod,
        data2024.ratePlanName
      )
    } yield chargeDistributionMultiplier(priceDistribution, priceCorrectionFactor)
  }

  def chargeDistributionToChargeOverrides(
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

  def zuoraUpdate(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
  ): Either[DataExtractionFailure, ZuoraSubscriptionUpdate] = {
    for {
      data2024 <- Estimation.subscriptionToSubscriptionData2024(subscription).left.map(DataExtractionFailure)
      chargeDistribution <- subscriptionToNewChargeDistribution2024(subscription).toRight(
        DataExtractionFailure("error")
      )
    } yield ZuoraSubscriptionUpdate(
      add = List(
        AddZuoraRatePlan(
          productRatePlanId = data2024.targetRatePlanId,
          contractEffectiveDate = effectiveDate,
          chargeOverrides =
            chargeDistributionToChargeOverrides(chargeDistribution, BillingPeriod.toString(data2024.billingPeriod))
        )
      ),
      remove = List(
        RemoveZuoraRatePlan(
          ratePlanId = data2024.ratePlan.id,
          effectiveDate
        )
      ),
      currentTerm = None,
      currentTermPeriodType = None
    )
  }
}
