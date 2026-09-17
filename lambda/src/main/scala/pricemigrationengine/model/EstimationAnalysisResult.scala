package pricemigrationengine.model

import java.time.LocalDate

sealed trait EstimationAnalysisResult
object EARClearance extends EstimationAnalysisResult
object EARMissingData extends EstimationAnalysisResult
object EARSubscriptionCancelled extends EstimationAnalysisResult
object EARSubscriptionAutoRenewFlagFalse extends EstimationAnalysisResult
object EARPrintWithZeroBillingPeriods extends EstimationAnalysisResult
object EARPrintWithTwoBillingPeriods extends EstimationAnalysisResult

case class CheckInput(subscription: ZuoraSubscription, today: LocalDate)

object EstimationAnalysisResult {

  def firstVetoElseDefault[A, T](a: A, fs: List[A => Option[T]], default: T): T = {
    // This evaluates the functions in order and return the `thing` from the first
    // Some(thing), and otherwise returns the default value
    fs.view
      .flatMap(f => f(a))
      .headOption
      .getOrElse(default)
  }

  def checkActiveRatePlanUniqueness(packet: CheckInput): Option[EstimationAnalysisResult] = {
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

  def checkSubscriptionStatus(packet: CheckInput): Option[EstimationAnalysisResult] = {
    if (packet.subscription.status == "Cancelled") {
      Some(EARSubscriptionCancelled)
    } else {
      None
    }
  }

  def checkSubscriptionAutoRenewFlag(packet: CheckInput): Option[EstimationAnalysisResult] = {
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
    val packet = CheckInput(subscription, today)

    val universalChecks: List[CheckInput => Option[EstimationAnalysisResult]] =
      List(checkSubscriptionStatus, checkSubscriptionAutoRenewFlag)

    val print2026Checks: List[CheckInput => Option[EstimationAnalysisResult]] =
      List(checkSubscriptionStatus, checkSubscriptionAutoRenewFlag, checkActiveRatePlanUniqueness)

    val checks = MigrationType(cohortSpec) match {
      case Test1                         => universalChecks
      case GuardianWeekly2025            => universalChecks
      case Newspaper2025P1               => universalChecks
      case Newspaper2025P3               => universalChecks
      case ProductMigration2025N4        => universalChecks
      case Membership2025                => universalChecks
      case DigiSubs2025                  => universalChecks
      case SupporterPlus2026             => universalChecks
      case Print2026C1GWAnnualsUK        => universalChecks
      case Print2026C1GWQuarterliesUK    => universalChecks
      case Print2026C1NPAnnualsUK        => print2026Checks
      case Print2026C1NPQuarterliesUK    => print2026Checks
      case Print2026C1NPSemiannualsUK    => print2026Checks
      case Print2026C2NPMonthliesUK      => print2026Checks
      case Print2026C3GWMonthliesUK      => universalChecks
      case Print2026C3NPMonthliesUK      => print2026Checks
      case Print2026C4NPMonthliesUK      => print2026Checks
      case Print2026C5GW                 => universalChecks
      case Print2026C5NP                 => print2026Checks
      case Print2026C6GWQuarterliesNonUK => universalChecks
    }

    firstVetoElseDefault(packet, checks, EARClearance)
  }
}
