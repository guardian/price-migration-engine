package pricemigrationengine.model

import pricemigrationengine.migrations.{
  DigiSubs2025Migration,
  GuardianWeekly2025Migration,
  Membership2025Migration,
  Newspaper2025P1Migration,
  ProductMigration2025N4Migration,
  SupporterPlus2026Migration
}

import java.time.LocalDate

object EstimationHandlerHelper {

  def earliestAmendmentEffectiveDate(cohortSpec: CohortSpec): LocalDate = {
    MigrationType(cohortSpec) match {
      case Test1                  => LocalDate.of(2025, 9, 10)
      case GuardianWeekly2025     => GuardianWeekly2025Migration.earliestAmendmentEffectiveDate
      case Newspaper2025P1        => Newspaper2025P1Migration.earliestAmendmentEffectiveDate
      case Newspaper2025P3        => Newspaper2025P1Migration.earliestAmendmentEffectiveDate
      case ProductMigration2025N4 => ProductMigration2025N4Migration.earliestAmendmentEffectiveDate
      case Membership2025         => Membership2025Migration.earliestAmendmentEffectiveDate
      case DigiSubs2025           => DigiSubs2025Migration.earliestAmendmentEffectiveDate
      case SupporterPlus2026      => SupporterPlus2026Migration.earliestAmendmentEffectiveDate
      case SupporterPlus2026N2    => SupporterPlus2026Migration.earliestAmendmentEffectiveDate
      case SupporterPlus2026N3    => SupporterPlus2026Migration.earliestAmendmentEffectiveDate
      case SupporterPlus2026N4    => SupporterPlus2026Migration.earliestAmendmentEffectiveDate
      case SupporterPlus2026N5    => SupporterPlus2026Migration.earliestAmendmentEffectiveDate
    }
  }

  def migrationCapRatio(cohortSpec: CohortSpec): Option[Double] = {
    // This is where we declare the optional capping of each migration
    MigrationType(cohortSpec) match {
      case Test1                  => None
      case GuardianWeekly2025     => Some(1.2)
      case Newspaper2025P1        => Some(1.2)
      case Newspaper2025P3        => Some(1.2)
      case ProductMigration2025N4 => None
      case Membership2025         => Some(1.43)
      case DigiSubs2025           => Some(1.25)
      case SupporterPlus2026      => None
      case SupporterPlus2026N2    => None
      case SupporterPlus2026N3    => None
      case SupporterPlus2026N4    => None
      case SupporterPlus2026N5    => None
    }
  }

  def commsPrice(cohortSpec: CohortSpec, oldPrice: BigDecimal, estimatedNewPriceUncapped: BigDecimal): BigDecimal = {
    PriceCap.cappedPrice(
      oldPrice,
      estimatedNewPriceUncapped,
      migrationCapRatio(cohortSpec: CohortSpec).map(ratio => BigDecimal(ratio))
    )
  }
}
