package pricemigrationengine.migrations
import pricemigrationengine.model._

import java.time.LocalDate

object Newspaper2024MigrationAmendment {

  def subscriptionToNewPriceDistribution(
      subscription: ZuoraSubscription
  ): Option[Newspaper2024MigrationStaticData.PriceDistribution] = {
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

  def subscriptionToZuoraSubscriptionUpdate(
      subscription: ZuoraSubscription,
      effectiveDate: LocalDate,
  ): Either[AmendmentDataFailure, ZuoraSubscriptionUpdate] = {

    Right(
      ZuoraSubscriptionUpdate(
        add = List(
          AddZuoraRatePlan(
            productRatePlanId = "2c92a0fd560d13880156136b72e50f0c",
            contractEffectiveDate = LocalDate.of(2024, 2, 23),
            List(
              ChargeOverride(
                productRatePlanChargeId = "2c92a0fd560d13880156136b74780f3f",
                billingPeriod = "Month",
                BigDecimal(10.24)
              ),
              ChargeOverride(
                productRatePlanChargeId = "2c92a0fd560d13880156136b74b80f47",
                billingPeriod = "Month",
                BigDecimal(13.89)
              )
            )
          )
        ),
        remove = List(
          RemoveZuoraRatePlan(
            ratePlanId = "2c92a0fd560d13880156136b72e50f0c",
            LocalDate.of(2024, 2, 23)
          )
        ),
        currentTerm = None,
        currentTermPeriodType = None
      )
    )
  }
}
