package pricemigrationengine.migrations

import pricemigrationengine.model.AmendmentData.RatePlanChargePair
import pricemigrationengine.model.ZuoraProductCatalogue.{homeDeliveryRatePlans, newGuardianWeeklyRatePlans}
import pricemigrationengine.model.{
  DataExtractionFailure,
  ZuoraAccount,
  ZuoraProductCatalogue,
  ZuoraProductRatePlan,
  ZuoraRatePlanCharge,
  Currency
}

/*
  Echo Legacy rate plans have to be migrated to use newer rate plans such as Weekend, Everyday, Sixday, Saturday and Sunday.
  Migrating these is not as easy as mapping the current rateplan Id with the new one in the catalogue, so we are taking a separate approach here.

  We see how many days in the echo-legacy plan are currently being charged, i.e.: has a price over 0, then retrieve the corresponding rate plan for the number of days needed.
 */
case class GuardianWeeklyMigration(productRatePlan: ZuoraProductRatePlan, chargePairs: Seq[RatePlanChargePair])
object GuardianWeeklyMigration {
  // we need to know:
  // billingPeriod - monthly, quarterly, annually
  // Whether it should change to Domestic or ROW
  // Need to consider delivery address: person could be paying in GBP with UK billing address but the paper could be delivered in a different country

  def getNewRatePlanCharges(
      account: ZuoraAccount,
      catalogue: ZuoraProductCatalogue,
      ratePlanCharges: Seq[ZuoraRatePlanCharge]
  ): Either[DataExtractionFailure, GuardianWeeklyMigration] = {
    val billingPeriod = ratePlanCharges.head.billingPeriod match {
      case Some(value) =>
        value match {
          case "Quarter" => "Quarterly"
          case "Month"   => "Monthly"
          case "Annual"  => "Annual"
          case default   => Left(DataExtractionFailure(s"billingPeriod is $default for ratePlan"))
        }

      case None => Left(DataExtractionFailure("billingPeriod is null for ratePlan"))
    }

    val guardianWeeklyRatePlans =
      newGuardianWeeklyRatePlans(catalogue)
    val deliveryCountry = account.soldToContact.country

    def fetchPlan(
        currentCharges: Seq[ZuoraRatePlanCharge],
        ratePlanName: String
    ): Either[DataExtractionFailure, GuardianWeeklyMigration] = {
      val newRatePlan = guardianWeeklyRatePlans
        .find(_.name == ratePlanName)
        .find(_.productRatePlanCharges.head.billingPeriod == currentCharges.head.billingPeriod)

      newRatePlan match {
        case Some(plan) =>
          val chargePairs =
            for ((chargeFromSub, catalogueCharge) <- currentCharges zip plan.productRatePlanCharges)
              yield RatePlanChargePair(chargeFromSub, catalogueCharge)

          Right(GuardianWeeklyMigration(plan, chargePairs))
        case None =>
          Left(
            DataExtractionFailure(
              s"Failed to find new rate plan for Guardian Weekly sub: $ratePlanName, ratePlanCharges: ${ratePlanCharges.mkString(", ")}"
            )
          )
      }
    }

    /*
      if USD {
        if deliveryAddress == 'United States' then 'Domestic' else 'ROW'
      } else {
        'Domestic'
      }
     */

    def aaaa(
        currency: Currency,
        ratePlanCharges: Seq[ZuoraRatePlanCharge]
    ): Either[DataExtractionFailure, GuardianWeeklyMigration] = {
      if (ratePlanCharges.head.currency == "USD") {
        for {
          ratePlan <-
            if (deliveryCountry == "United States" || deliveryCountry == "USA")
              fetchPlan(ratePlanCharges, s"GW Oct 18 - $billingPeriod - Domestic")
            else fetchPlan(ratePlanCharges, s"GW Oct 18 - $billingPeriod - ROW")
        } yield ratePlan
      } else {
        fetchPlan(ratePlanCharges, s"GW Oct 18 - $billingPeriod - Domestic")
      }
    }

    for {
      ratePlanChargePairs <- aaaa(ratePlanCharges.head.currency, ratePlanCharges)
    } yield ratePlanChargePairs
  }
}
