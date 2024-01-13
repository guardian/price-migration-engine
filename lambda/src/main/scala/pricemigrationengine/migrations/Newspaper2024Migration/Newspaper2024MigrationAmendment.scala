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

}
