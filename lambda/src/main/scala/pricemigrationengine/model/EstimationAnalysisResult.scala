package pricemigrationengine.model

import java.time.LocalDate

sealed trait EstimationAnalysisResult
object EARClearance extends EstimationAnalysisResult
object EARMissingData extends EstimationAnalysisResult
object EARPrintWithZeroBillingPeriods extends EstimationAnalysisResult
object EARPrintWithTwoBillingPeriods extends EstimationAnalysisResult

object EstimationAnalysisResult {

  def printProduct2026EstimationAnalysis(
      subscription: ZuoraSubscription,
      today: LocalDate
  ): EstimationAnalysisResult = {
    val sizeOpt: Option[Int] = for {
      ratePlan <- SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(
        subscription,
        today
      )
    } yield ZuoraRatePlan.ratePlanToChargesBillingPeriods(ratePlan).distinct.length
    sizeOpt match {
      case None    => EARMissingData
      case Some(0) => EARPrintWithZeroBillingPeriods
      case Some(1) => EARClearance
      case _       => EARPrintWithTwoBillingPeriods
    }
  }

  def subscriptionEstimationAnalysis(
      cohortSpec: CohortSpec,
      subscription: ZuoraSubscription,
      today: LocalDate
  ): EstimationAnalysisResult = {
    MigrationType(cohortSpec) match {
      case Test1                         => EARClearance
      case GuardianWeekly2025            => EARClearance
      case Newspaper2025P1               => EARClearance
      case Newspaper2025P3               => EARClearance
      case ProductMigration2025N4        => EARClearance
      case Membership2025                => EARClearance
      case DigiSubs2025                  => EARClearance
      case SupporterPlus2026             => EARClearance
      case Print2026C1GWAnnualsUK        => EARClearance
      case Print2026C1GWQuarterliesUK    => EARClearance
      case Print2026C1NPAnnualsUK        => printProduct2026EstimationAnalysis(subscription, today)
      case Print2026C1NPQuarterliesUK    => printProduct2026EstimationAnalysis(subscription, today)
      case Print2026C1NPSemiannualsUK    => printProduct2026EstimationAnalysis(subscription, today)
      case Print2026C2NPMonthliesUK      => printProduct2026EstimationAnalysis(subscription, today)
      case Print2026C3GWMonthliesUK      => EARClearance
      case Print2026C3NPMonthliesUK      => printProduct2026EstimationAnalysis(subscription, today)
      case Print2026C4NPMonthliesUK      => printProduct2026EstimationAnalysis(subscription, today)
      case Print2026C5GW                 => EARClearance
      case Print2026C5NP                 => printProduct2026EstimationAnalysis(subscription, today)
      case Print2026C6GWQuarterliesNonUK => EARClearance
    }
  }
}
