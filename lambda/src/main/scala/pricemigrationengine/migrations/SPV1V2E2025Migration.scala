package pricemigrationengine.migrations
import pricemigrationengine.model.ZuoraRatePlan
import pricemigrationengine.model._

import java.time.LocalDate

object SPV1V2E2025Migration {
  val maxLeadTime = 35
  val minLeadTime = 32

  def getSupporterPlusV1RatePlan(subscription: ZuoraSubscription): Either[DataExtractionFailure, ZuoraRatePlan] = {
    subscription.ratePlans.find(rp =>
      rp.ratePlanName.contains("Supporter Plus") && !rp.lastChangeType.contains("Remove")
    ) match {
      case None =>
        Left(
          DataExtractionFailure(
            s"Subscription ${subscription.subscriptionNumber} doesn't have any `Add`ed rate plan with pattern `Supporter Plus`"
          )
        )
      case Some(ratePlan) => Right(ratePlan)
    }
  }

  def getRatePlanCharge(
      subscriptionNumber: String,
      ratePlan: ZuoraRatePlan
  ): Either[DataExtractionFailure, ZuoraRatePlanCharge] = {
    ratePlan.ratePlanCharges.find(rpc => rpc.name.contains("Supporter Plus")) match {
      case None => {
        Left(
          DataExtractionFailure(s"Subscription ${subscriptionNumber} has a rate plan (${ratePlan}), but with no charge")
        )
      }
      case Some(ratePlanCharge) => Right(ratePlanCharge)
    }
  }

  // -----------------------------
  // Migration Interface
  // -----------------------------

  def priceData(subscription: ZuoraSubscription): Either[Failure, PriceData] = {
    for {
      ratePlan <- getSupporterPlusV1RatePlan(subscription)
      billingPeriod <- ZuoraRatePlan.ratePlanToBillingPeriod(ratePlan).toRight(DataExtractionFailure(""))
      ratePlanCharge <- getRatePlanCharge(subscription.subscriptionNumber, ratePlan)
      currency = ratePlanCharge.currency
    } yield {
      // Since this is a subscription restructuration and not a price migration, we
      // have a much easier time here than for a regular migration. We do not actually
      // care what the prices before and after are. The split will be done for the Amendment step.
      // Note that the Estimation step has been updated to not perform the price
      // comparison check.
      PriceData(currency, BigDecimal(0), BigDecimal(0), billingPeriod.toString)
    }
  }

  // This need to be implemented before the end of Feb 2025
  def zuoraUpdate(
      subscriptionBeforeUpdate: ZuoraSubscription,
      startDate: LocalDate
  ): Either[DataExtractionFailure, ZuoraSubscriptionUpdate] = ???

}
