package pricemigrationengine.model

import java.time.LocalDate

sealed trait RateplansProbeResult
object ShouldProceed extends RateplansProbeResult
object ShouldCancel extends RateplansProbeResult
object IndeterminateConclusion extends RateplansProbeResult

object RateplansProbe {

  /*
    Date: 07th February 2024

    This object implements the logic that decides whether a subscription
    that is just about to be notified should indeed be notified and amended and that no
    migration cancelling events have occurred on the subscription since it was estimated.

    This check is necessary because migration cohort items can sleep for a long time (months) and
    the engine is not kept up to date with any possible changes that could have occurred on the subscription

    In effect we are replacing the work that was done here:
    "Introduce last amendment check": https://github.com/guardian/price-migration-engine/pull/925

    We are also going to slightly change the moment this check is performed. Since subscriptions
    sleep before the notification step (and not before the amendment step), we should perform that check before the
    Notification step is performed

    The main logic used in RateplansProbe is that since we cannot have programmatic (API) access
    to the list of all amendments of a subscription, then we are going to work off the subscription
    itself by probing its rate plans.

    ( Zuora offers the retrieval of the last one, but not the entire list;
    see: https://www.zuora.com/developer/api-references/older-api/operation/GET_AmendmentsBySubscriptionID/ )

    During testing we discovered that in fact all the information we need is carried by the subscription
    itself and that a relatively simple lookup of the rate plans should enable us to make the
    same determination we would have been able to make if we could analyse the set of amendments.

    Last but not least, by design, the current implementation is not perfect, in the sense that
    if the code sees a subscription that contains a rate plan that is not one of the type it know
    how to deal with, then the probe function will return IndeterminateConclusion causing the
    engine to immediately fail. The engine operators should update the code to handle the missing case.
    We did a separate extensive testing on a large sample of subscriptions to ensure we would be automatically
    covering most cases.
   */

  // All the functions of this object are pure and strive to work without side effects

  def ratePlanDate(ratePlan: ZuoraRatePlan): Option[LocalDate] = {
    // This function takes a rate plan and tries and determine its creation date
    // The logic came from the work done here: https://github.com/guardian/price-migration-engine/pull/974
    ratePlan.ratePlanCharges.headOption.flatMap(ratePlanCharge => ratePlanCharge.originalOrderDate)
  }

  def addedRatePlansAfterDate(subscription: ZuoraSubscription, date: LocalDate): List[ZuoraRatePlan] = {
    // This function takes a subscription and return the list of rate plans added to the subscription
    // after a given date. (In normal circumstances that date is intended to be the date the subscription
    // has been estimated.)
    subscription.ratePlans
      .filter(ratePlan => ratePlan.lastChangeType.fold(false)(_ == "Add"))
      .filter(ratePlan => ratePlanDate(ratePlan: ZuoraRatePlan).fold(false)(_.isAfter(date)))
  }

  def selectNonTrivialRatePlans(ratePlans: List[ZuoraRatePlan]): List[ZuoraRatePlan] = {
    // This function should be used in conjunction with addedRatePlansAfterDate
    // The intent is to remove from a list of rate plans the rate plans that we know
    // are harmless and do not change the readiness of a subscription for price migration
    // We refer to those harmless rate plans as "trivial"

    // At the moment, we are filtering away "Discounts"
    ratePlans.filter(ratePlan => ratePlan.productName != "Discounts")
  }

  def probe(subscription: ZuoraSubscription, date: LocalDate): RateplansProbeResult = {
    // This function takes a subscription (in intended circumstances that would be a subscription that has slept for
    // a while and is about to be notified+amended) and decides whether or not to pursue. The return value is not a Boolean
    // but a RateplansProbeResult. The interesting case is IndeterminateConclusion which will cause the engine to fail
    // This is on purpose and by design. Upon such a failure the engine operators should look up the subscription,
    // identify the type of rate plan that filtered away by selectNonTrivialRatePlans and decide the correct
    // course of action.

    if (subscription.status == "Cancelled") {
      ShouldCancel
    } else {
      if (selectNonTrivialRatePlans(addedRatePlansAfterDate(subscription, date)).isEmpty) {
        ShouldProceed
      } else {
        IndeterminateConclusion
      }
    }
  }
}
