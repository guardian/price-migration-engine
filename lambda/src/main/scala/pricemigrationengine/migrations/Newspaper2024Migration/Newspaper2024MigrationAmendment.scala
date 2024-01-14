package pricemigrationengine.migrations
import pricemigrationengine.migrations.Newspaper2024MigrationEstimation.SubscriptionData2024
import pricemigrationengine.migrations.Newspaper2024MigrationStaticData._
import pricemigrationengine.model._

import java.time.LocalDate

object Newspaper2024MigrationAmendment {

  def subscriptionToNewChargeDistribution2024(subscription: ZuoraSubscription): Option[ChargeDistribution2024] = {
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

  def subscriptionToZuoraSubscriptionUpdate(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    def transform1[T](option: Option[T]): Either[AmendmentDataFailure, T] = {
      option match {
        case None                 => Left(AmendmentDataFailure("error"))
        case Some(ratePlanCharge) => Right(ratePlanCharge)
      }
    }

    def transform2[T](data: Either[String, T]): Either[AmendmentDataFailure, T] = {
      data match {
        case Left(string) => Left(AmendmentDataFailure(string))
        case Right(t)     => Right(t)
      }
    }

    for {
      data2024 <- transform2[SubscriptionData2024](
        Newspaper2024MigrationEstimation.subscriptionToSubscriptionData2024(subscription)
      )
      chargeDistribution <- transform1[ChargeDistribution2024](subscriptionToNewChargeDistribution2024(subscription))
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
