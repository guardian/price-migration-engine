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
      //
      // 19 October 2026 (first day of notifications: 14 September 2026)
      case Print2026C1GWAnnualsUK => LocalDate.of(2026, 10, 19)
      //
      // 19 October 2026 (first day of notifications: 14 September 2026)
      case Print2026C1GWQuarterliesUK => LocalDate.of(2026, 10, 19)
      //
      // 19 October 2026 (first day of notifications: 14 September 2026)
      case Print2026C1NPAnnualsUK => LocalDate.of(2026, 10, 19)
      //
      // 19 October 2026 (first day of notifications: 14 September 2026)
      case Print2026C1NPQuarterliesUK => LocalDate.of(2026, 10, 19)
      //
      // 19 October 2026 (first day of notifications: 14 September 2026)
      case Print2026C1NPSemiannualsUK => LocalDate.of(2026, 10, 19)
      //
      // 16 November 2026 (first day of notifications: 12 October 2026)
      case Print2026C2NPMonthliesUK => LocalDate.of(2026, 11, 16)
      //
      // 23 November 2026 (first day of notifications: 19 October 2026)
      case Print2026C3GWMonthliesUK => LocalDate.of(2026, 11, 23)
      //
      // 23 November 2026 (first day of notifications: 19 October 2026)
      case Print2026C3NPMonthliesUK => LocalDate.of(2026, 11, 23)
      //
      // 4 December 2026 (first day of notifications: 30 October 2026)
      case Print2026C4NPMonthliesUK => LocalDate.of(2026, 12, 4)
      //
      // 22 January 2027 (first day of notifications: 14 December 2026)
      case Print2026C5GW => LocalDate.of(2027, 1, 22)
      //
      // 22 January 2027 (first day of notifications: 14 December 2026)
      case Print2026C5NP => LocalDate.of(2027, 1, 22)
      //
      // 8 February 2027 (first day of notifications: 4 January 2027)
      case Print2026C6GWQuarterliesNonUK => LocalDate.of(2027, 2, 8)
    }
  }

  def migrationCapRatio(cohortSpec: CohortSpec): Option[Double] = {
    // This is where we declare the optional capping of each migration
    MigrationType(cohortSpec) match {
      case Test1                         => None
      case GuardianWeekly2025            => Some(1.2)
      case Newspaper2025P1               => Some(1.2)
      case Newspaper2025P3               => Some(1.2)
      case ProductMigration2025N4        => None
      case Membership2025                => Some(1.43)
      case DigiSubs2025                  => Some(1.25)
      case SupporterPlus2026             => None
      case SupporterPlus2026N2           => None
      case SupporterPlus2026N3           => None
      case SupporterPlus2026N4           => None
      case SupporterPlus2026N5           => None
      case Print2026C1GWAnnualsUK        => Some(1.10) // GuardianWeekly 10%
      case Print2026C1GWQuarterliesUK    => Some(1.10) // GuardianWeekly 10%
      case Print2026C1NPAnnualsUK        => Some(1.071) // Newspaper 7.1%
      case Print2026C1NPQuarterliesUK    => Some(1.071) // Newspaper 7.1%
      case Print2026C1NPSemiannualsUK    => Some(1.071) // Newspaper 7.1%
      case Print2026C2NPMonthliesUK      => Some(1.071) // Newspaper 7.1%
      case Print2026C3GWMonthliesUK      => Some(1.10) // GuardianWeekly 10%
      case Print2026C3NPMonthliesUK      => Some(1.071) // Newspaper 7.1%
      case Print2026C4NPMonthliesUK      => Some(1.071) // Newspaper 7.1%
      case Print2026C5GW                 => Some(1.10) // GuardianWeekly 10%
      case Print2026C5NP                 => Some(1.071) // Newspaper 7.1%
      case Print2026C6GWQuarterliesNonUK => Some(1.10) // GuardianWeekly 10%
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
