package pricemigrationengine.model

import pricemigrationengine.migrations.T9xGWRatePlanIds

import scala.math.BigDecimal.RoundingMode

// T6xLegChargeOverrides carries the information needed to build a charge overrides as part
// of making the amendment payload of the Orders API. It can be thought of as an intermediary
// representation between the Guardian Weekly (GuardianWeeklyLegPercentageDistribution) and the
// Newspaper (NewspaperLegPercentageDistribution) pricing percentage distributions,
// and the Value used in the JSON payload

case class T6xLegChargeOverride(productRatePlanChargeId: String, price: BigDecimal, billingPeriod: BillingPeriod)

object T6xLegChargeOverride {

  def ensureTotal(legs: List[T6xLegChargeOverride], targetTotalPrice: BigDecimal): List[T6xLegChargeOverride] = {
    // This function performs a similar calibration operation as
    // ZuoraOrdersApiPrimitives.ratePlanChargesToChargeOverrides, but on Scala types
    // and not JSON Values. Its purpose is to ensure that the sum of prices is exactly the
    // total that was provided by Marketing, and that we are not off by pennies due to the
    // RoundingMode

    val currentTotal = legs.map(l => l.price).sum

    if ((currentTotal - targetTotalPrice).abs < 0.001) {
      // The current total is equal to the target total, so we can return the legs
      // as given to the function
      legs
    } else {
      // Here we have a discrepancy

      val difference = targetTotalPrice - currentTotal
      // As computed the difference is what we need to add to one of the legs to reach parity with the targetTotalPrice
      // Let's add it to the first leg

      val firstLeg = legs.take(1).map(l => l.copy(price = l.price + difference))
      firstLeg ++ legs.drop(1)
    }
  }

  def decideT6xLegChargeOverridesGuardianWeekly(
      distribution: T5xFinanceAllocation,
      gwRatePlanIds: T9xGWRatePlanIds,
      billingPeriod: BillingPeriod,
      targetPrice: BigDecimal,
  ): List[T6xLegChargeOverride] = {
    // Decide T6xLegChargeOverrides in the case of Guardian Weekly subs

    // It's useful here to understand why the signature of this function is the way it is

    // The `distribution` comes from knowing which type of subscription we are dealing with
    // For instance: (Monthly, "GBP", Domestic) simply maps to T5xDistribution(BigDecimal(60.5), BigDecimal(39.5))
    // by the `getDistribution` look up.

    // The `gwRatePlanIds` is simply constructed for the current state
    // Simple contains the Ids from the product catalogue.
    // When this function was first written we were doing a product migration
    // from no longer to use GW rate plans, to the new Legacy rate plans I
    // introduced in Sept 2026.
    // I am keeping this functions in this module, because it goes well with
    // `decideT6xLegChargeOverridesNewspaper` but one day we might move both
    // to a better location; possibly to the migration modules themselves, and
    // call `ensureTotal` from there.

    // The billing period is also read from the subscription

    // The target price is the price we are moving to.

    // It's interesting to see who owns which parameter
    // distribution                   : Finance
    // productRatePlanChargeIdMapping : Zuora (current state of the sub)
    // billingPeriod                  : Zuora (current state of the sub)
    // targetPrice                    : Marketing

    val legs = List(
      T6xLegChargeOverride(
        gwRatePlanIds.gwChargeId,
        (targetPrice * distribution.guardianWeeklyPercentage * 0.01).setScale(2, RoundingMode.DOWN),
        billingPeriod
      ),
      T6xLegChargeOverride(
        gwRatePlanIds.dpChargeId,
        (targetPrice * distribution.digitalPackPercentage * 0.01).setScale(2, RoundingMode.DOWN),
        billingPeriod
      )
    )

    // We now need to ensure that we are recovering the extact target price, despite the two
    // .setScale(2, RoundingMode.DOWN)
    ensureTotal(legs, targetPrice)
  }

  def decideT6xLegChargeOverridesNewspaper(
      distribution: List[T4xLeg],
      productRatePlanChargeIdMapping: Map[T1xNewspaperLegType, String],
      billingPeriod: BillingPeriod,
      targetPrice: BigDecimal
  ): Option[List[T6xLegChargeOverride]] = {
    // Decide T6xLegChargeOverrides in the case of Newspaper subs
    // Interesting differences between this variant and the previous one
    // - `distributions` is now a List[T4xLegPercentage] since that's how we get them from the Finance data
    // - `productRatePlanChargeIdMapping` maps T1xNewspaperPackageLegs to String

    val legs = distribution.flatMap(t4 => {
      for {
        chargeId <- productRatePlanChargeIdMapping.get(t4.legType)
      } yield T6xLegChargeOverride(
        productRatePlanChargeId = chargeId,
        (targetPrice * t4.percentage * 0.01).setScale(2, RoundingMode.DOWN),
        billingPeriod
      )
    })

    // We only pursue if we haven't lost any data (which would only happen if
    // the productRatePlanChargeIdMapping wasn't complete)
    // We check that with the size of legs compared to distribution

    if (legs.length < distribution.length) {
      None
    } else {
      // We now need to ensure that we are recovering the extact target price, despite the two
      // .setScale(2, RoundingMode.DOWN)
      Some(ensureTotal(legs, targetPrice))
    }
  }
}
