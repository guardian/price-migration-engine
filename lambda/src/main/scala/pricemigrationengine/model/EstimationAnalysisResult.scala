package pricemigrationengine.model

import java.time.LocalDate

sealed trait EstimationAnalysisResult
object EARClearance extends EstimationAnalysisResult
object EARMissingData extends EstimationAnalysisResult
object EARSubscriptionCancelled extends EstimationAnalysisResult
object EARSubscriptionAutoRenewFlagFalse extends EstimationAnalysisResult
object EARPrintWithZeroBillingPeriods extends EstimationAnalysisResult
object EARPrintWithTwoBillingPeriods extends EstimationAnalysisResult

case class DataPacket(subscription: ZuoraSubscription, today: LocalDate)

object EstimationAnalysisResult {

  def firstMatch[A, T](a: A, fs: List[A => Option[T]], default: T): T = {
    // This evaluates the functions in order and return the `thing` from the first
    // Some(thing), and otherwise returns the default value
    fs.iterator.map(_(a)).collectFirst { case Some(t) => t }.getOrElse(default)
  }

  def checkActiveRatePlanUniqueness(packet: DataPacket): Option[EstimationAnalysisResult] = {
    val sizeOpt: Option[Int] = for {
      ratePlan <- SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(
        packet.subscription,
        packet.today
      )
    } yield ZuoraRatePlan.ratePlanToChargesBillingPeriods(ratePlan).distinct.length
    sizeOpt match {
      case None    => Some(EARMissingData)
      case Some(0) => Some(EARPrintWithZeroBillingPeriods)
      case Some(1) => None
      case _       => Some(EARPrintWithTwoBillingPeriods)
    }
  }

  def checkSubscriptionStatus(packet: DataPacket): Option[EstimationAnalysisResult] = {
    if (packet.subscription.status == "Cancelled") {
      Some(EARSubscriptionCancelled)
    } else {
      None
    }
  }

  def checkSubscriptionAutoRenewFlag(packet: DataPacket): Option[EstimationAnalysisResult] = {
    if (packet.subscription.autoRenew) {
      None
    } else {
      Some(EARSubscriptionAutoRenewFlagFalse)
    }
  }

  def subscriptionEstimationAnalysis(
      cohortSpec: CohortSpec,
      subscription: ZuoraSubscription,
      today: LocalDate
  ): EstimationAnalysisResult = {
    val packet = DataPacket(subscription, today)

    val universalChecks: List[DataPacket => Option[EstimationAnalysisResult]] =
      List(checkSubscriptionStatus, checkSubscriptionAutoRenewFlag)

    val print2026Checks: List[DataPacket => Option[EstimationAnalysisResult]] =
      List(checkSubscriptionStatus, checkSubscriptionAutoRenewFlag, checkActiveRatePlanUniqueness)

    MigrationType(cohortSpec) match {
      case Test1                         => firstMatch(packet, universalChecks, EARClearance)
      case GuardianWeekly2025            => firstMatch(packet, universalChecks, EARClearance)
      case Newspaper2025P1               => firstMatch(packet, universalChecks, EARClearance)
      case Newspaper2025P3               => firstMatch(packet, universalChecks, EARClearance)
      case ProductMigration2025N4        => firstMatch(packet, universalChecks, EARClearance)
      case Membership2025                => firstMatch(packet, universalChecks, EARClearance)
      case DigiSubs2025                  => firstMatch(packet, universalChecks, EARClearance)
      case SupporterPlus2026             => firstMatch(packet, universalChecks, EARClearance)
      case Print2026C1GWAnnualsUK        => firstMatch(packet, universalChecks, EARClearance)
      case Print2026C1GWQuarterliesUK    => firstMatch(packet, universalChecks, EARClearance)
      case Print2026C1NPAnnualsUK        => firstMatch(packet, print2026Checks, EARClearance)
      case Print2026C1NPQuarterliesUK    => firstMatch(packet, print2026Checks, EARClearance)
      case Print2026C1NPSemiannualsUK    => firstMatch(packet, print2026Checks, EARClearance)
      case Print2026C2NPMonthliesUK      => firstMatch(packet, print2026Checks, EARClearance)
      case Print2026C3GWMonthliesUK      => firstMatch(packet, universalChecks, EARClearance)
      case Print2026C3NPMonthliesUK      => firstMatch(packet, print2026Checks, EARClearance)
      case Print2026C4NPMonthliesUK      => firstMatch(packet, print2026Checks, EARClearance)
      case Print2026C5GW                 => firstMatch(packet, universalChecks, EARClearance)
      case Print2026C5NP                 => firstMatch(packet, print2026Checks, EARClearance)
      case Print2026C6GWQuarterliesNonUK => firstMatch(packet, universalChecks, EARClearance)
    }
  }
}
