package pricemigrationengine.model

import pricemigrationengine.model.AmendmentData.RatePlanChargePair
import pricemigrationengine.model.ZuoraProductCatalogue.homeDeliveryRatePlans

/*
  Echo Legacy rate plans have to be migrated to use newer rate plans such as Weekend, Everyday, Sixday, Saturday and Sunday.
  Migrating these is not as easy as mapping the current rateplan Id with the new one in the catalogue, so we are taking a separate approach here.

  We see how many days in the echo-legacy plan are currently being charged, i.e.: has a price over 0, then retrieve the corresponding rate plan for the number of days needed.
 */
case class EchoLegacy(productRatePlan: ZuoraProductRatePlan, chargePairs: Seq[RatePlanChargePair])
object EchoLegacy {

  def getNewRatePlans(
      catalogue: ZuoraProductCatalogue,
      ratePlanCharges: Seq[ZuoraRatePlanCharge]
  ): Either[AmendmentDataFailure, EchoLegacy] = {

    def getNewPlan(
        ratePlanName: String,
        chargedDays: Seq[ZuoraRatePlanCharge]
    ): Either[AmendmentDataFailure, EchoLegacy] = {
      val deliveryRatePlans = homeDeliveryRatePlans(catalogue)
      val deliveryRatePlan = deliveryRatePlans.find(_.name == ratePlanName)

      deliveryRatePlan match {
        case Some(plan) =>
          val chargePairs =
            for ((planFromSub, cataloguePlan) <- chargedDays zip plan.productRatePlanCharges)
              yield RatePlanChargePair(planFromSub, cataloguePlan)

          Right(EchoLegacy(plan, chargePairs))
        case None =>
          Left(
            AmendmentDataFailure(
              s"Failed to find new rate plan for Echo-Legacy sub: $ratePlanName, ratePlanCharges: ${ratePlanCharges.mkString(", ")}"
            )
          )
      }
    }

    val echoLegacy = {
      val chargedDays = ratePlanCharges.filter(_.price > Some(0.0))

      chargedDays.length match {
        case 7 => getNewPlan("Everyday", chargedDays)
        case 6 if chargedDays.filter(_.name == "Sunday").isEmpty =>
          getNewPlan("Sixday", chargedDays)
        case 2 if chargedDays.filter(plan => plan.name == "Saturday" || plan.name == "Sunday").length == 2 =>
          getNewPlan("Weekend", chargedDays)
        case 1 =>
          chargedDays.head.name match {
            case "Saturday" => getNewPlan("Saturday", chargedDays)
            case "Sunday"   => getNewPlan("Sunday", chargedDays)
          }
        case _ =>
          Left(
            AmendmentDataFailure(
              s"Migration from Echo-Legacy plan failed for rate plan charges: ${ratePlanCharges.mkString(", ")}"
            )
          )
      }
    }

    echoLegacy
  }
}
