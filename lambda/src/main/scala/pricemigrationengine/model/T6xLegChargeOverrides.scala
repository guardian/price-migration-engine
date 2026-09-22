package pricemigrationengine.model

import scala.math.BigDecimal.RoundingMode

// T6xLegChargeOverrides carries the information needed to build a charge overrides as part
// of making the amendment payload of the Orders API. It can be thought of as an intermediary
// representation between the Guardian Weekly (GuardianWeeklyLegPercentageDistribution) and the
// Newspaper (NewspaperLegPercentageDistribution) pricing percentage distributions,
// and the Value used in the JSON payload

case class T6xLegChargeOverrides(productRatePlanChargeId: String, price: BigDecimal, billingPeriod: BillingPeriod)

object T6xLegChargeOverrides {

  def ensureTotal(legs: List[T6xLegChargeOverrides], targetTotalPrice: BigDecimal): List[T6xLegChargeOverrides] = {
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

  // Decide T6xLegChargeOverrides in the case of Guardian Weekly subs
  def decideT6xLegChargeOverrides(
      distribution: T5xDistribution,
      productRatePlanChargeIdMapping: Map[T7xGWSubLegs, String],
      billingPeriod: BillingPeriod,
      targetPrice: BigDecimal,
  ): Option[List[T6xLegChargeOverrides]] = {
    // It's useful here to understand why the signature of this function is the way it is

    // The `distribution` comes from knowing which type of subscription we are dealing with
    // For instance: (Monthly, "GBP", Domestic) simply maps to T5xDistribution(BigDecimal(60.5), BigDecimal(39.5))
    // by the `getDistribution` look up.

    // The `productRatePlanChargeIdMapping` is simply constructed for the current state
    // of the subscription. We construct it to avoid having rate plan ids hardcoded anywhere in the code
    // This means that we have a more generic handling of subscriptions regardless of the exact rate plan
    // they are coming from or going to.

    // The billing period is also read from the subscription

    // The target price is the price we are moving to.

    // It's interesting to see who owns which parameter
    // distribution                   : Finance
    // productRatePlanChargeIdMapping : Zuora (current state of the sub)
    // billingPeriod                  : Zuora (current state of the sub)
    // targetPrice                    : Marketing

    for {
      guardianWeeklyLegRatePlanChargeId <- productRatePlanChargeIdMapping.get(T7xGuardianWeekly)
      digitalPackPercentage <- productRatePlanChargeIdMapping.get(T7xDigitalPack)
    } yield {
      val legs = List(
        T6xLegChargeOverrides(
          guardianWeeklyLegRatePlanChargeId,
          (targetPrice * distribution.guardianWeeklyPercentage * 0.01).setScale(2, RoundingMode.DOWN),
          billingPeriod
        ),
        T6xLegChargeOverrides(
          digitalPackPercentage,
          (targetPrice * distribution.digitalPackPercentage * 0.01).setScale(2, RoundingMode.DOWN),
          billingPeriod
        )
      )

      // We now need to ensure that we are recovering the extact target price, despite the two
      // .setScale(2, RoundingMode.DOWN)
      ensureTotal(legs, targetPrice)
    }
  }

  // Decide T6xLegChargeOverrides in the case of Newspaper subs
  // Interesting differences between this variant and the previous one
  // - `distributions` is now a List[T4xLegPercentage] since that's how we get them from the Finance data
  // - `productRatePlanChargeIdMapping` maps T1xNewspaperPackageLegs to String
  def decideT6xLegChargeOverrides(
      distribution: List[T4xLeg],
      productRatePlanChargeIdMapping: Map[T1xNewspaperLegType, String],
      billingPeriod: BillingPeriod,
      targetPrice: BigDecimal
  ): Option[List[T6xLegChargeOverrides]] = {
    val legs = distribution.flatMap(t4 => {
      for {
        chargeId <- productRatePlanChargeIdMapping.get(t4.legType)
      } yield T6xLegChargeOverrides(
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
