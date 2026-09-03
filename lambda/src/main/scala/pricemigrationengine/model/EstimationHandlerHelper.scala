package pricemigrationengine.model

import pricemigrationengine.migrations.{
  DigiSubs2025Migration,
  GuardianWeekly2025Migration,
  Membership2025Migration,
  Newspaper2025P1Migration,
  Print2026C1GWAnnualsUKMigration,
  Print2026C1GWQuarterliesUKMigration,
  Print2026C1NPAnnualsUKMigration,
  Print2026C1NPQuarterliesUKMigration,
  Print2026C1NPSemiannualsUKMigration,
  Print2026C2NPMonthliesUKMigration,
  Print2026C3GWMonthliesUKMigration,
  Print2026C3NPMonthliesUKMigration,
  Print2026C4NPMonthliesUKMigration,
  Print2026C5GWMonthliesAnnualsNoEmailsNonUKMigration,
  Print2026C5NPNoEmailsUKMigration,
  Print2026C6GWQuarterliesNonUKMigration,
  ProductMigration2025N4Migration,
  SupporterPlus2026Migration
}

import java.time.LocalDate

object EstimationHandlerHelper {

  def earliestAmendmentEffectiveDate(cohortSpec: CohortSpec): LocalDate = {
    MigrationType(cohortSpec) match {
      case Test1                      => LocalDate.of(2025, 9, 10)
      case GuardianWeekly2025         => GuardianWeekly2025Migration.earliestAmendmentEffectiveDate
      case Newspaper2025P1            => Newspaper2025P1Migration.earliestAmendmentEffectiveDate
      case Newspaper2025P3            => Newspaper2025P1Migration.earliestAmendmentEffectiveDate
      case ProductMigration2025N4     => ProductMigration2025N4Migration.earliestAmendmentEffectiveDate
      case Membership2025             => Membership2025Migration.earliestAmendmentEffectiveDate
      case DigiSubs2025               => DigiSubs2025Migration.earliestAmendmentEffectiveDate
      case SupporterPlus2026          => SupporterPlus2026Migration.earliestAmendmentEffectiveDate
      case SupporterPlus2026N2        => SupporterPlus2026Migration.earliestAmendmentEffectiveDate
      case SupporterPlus2026N3        => SupporterPlus2026Migration.earliestAmendmentEffectiveDate
      case SupporterPlus2026N4        => SupporterPlus2026Migration.earliestAmendmentEffectiveDate
      case SupporterPlus2026N5        => SupporterPlus2026Migration.earliestAmendmentEffectiveDate
      case Print2026C1GWAnnualsUK     => Print2026C1GWAnnualsUKMigration.earliestAmendmentEffectiveDate
      case Print2026C1GWQuarterliesUK => Print2026C1GWQuarterliesUKMigration.earliestAmendmentEffectiveDate
      case Print2026C1NPAnnualsUK     => Print2026C1NPAnnualsUKMigration.earliestAmendmentEffectiveDate
      case Print2026C1NPQuarterliesUK => Print2026C1NPQuarterliesUKMigration.earliestAmendmentEffectiveDate
      case Print2026C1NPSemiannualsUK => Print2026C1NPSemiannualsUKMigration.earliestAmendmentEffectiveDate
      case Print2026C2NPMonthliesUK   => Print2026C2NPMonthliesUKMigration.earliestAmendmentEffectiveDate
      case Print2026C3GWMonthliesUK   => Print2026C3GWMonthliesUKMigration.earliestAmendmentEffectiveDate
      case Print2026C3NPMonthliesUK   => Print2026C3NPMonthliesUKMigration.earliestAmendmentEffectiveDate
      case Print2026C4NPMonthliesUK   => Print2026C4NPMonthliesUKMigration.earliestAmendmentEffectiveDate
      case Print2026C5GWMonthliesAnnualsNoEmailsNonUK =>
        Print2026C5GWMonthliesAnnualsNoEmailsNonUKMigration.earliestAmendmentEffectiveDate
      case Print2026C5NPNoEmailsUK       => Print2026C5NPNoEmailsUKMigration.earliestAmendmentEffectiveDate
      case Print2026C6GWQuarterliesNonUK => Print2026C6GWQuarterliesNonUKMigration.earliestAmendmentEffectiveDate
    }
  }

  def migrationCapRatio(cohortSpec: CohortSpec): Option[Double] = {
    // This is where we declare the optional capping of each migration
    MigrationType(cohortSpec) match {
      case Test1                                      => None
      case GuardianWeekly2025                         => Some(1.2)
      case Newspaper2025P1                            => Some(1.2)
      case Newspaper2025P3                            => Some(1.2)
      case ProductMigration2025N4                     => None
      case Membership2025                             => Some(1.43)
      case DigiSubs2025                               => Some(1.25)
      case SupporterPlus2026                          => None
      case SupporterPlus2026N2                        => None
      case SupporterPlus2026N3                        => None
      case SupporterPlus2026N4                        => None
      case SupporterPlus2026N5                        => None
      case Print2026C1GWAnnualsUK                     => Some(1.10) // GuardianWeekly 10%
      case Print2026C1GWQuarterliesUK                 => Some(1.10) // GuardianWeekly 10%
      case Print2026C1NPAnnualsUK                     => Some(1.071) // Newspaper 7.1%
      case Print2026C1NPQuarterliesUK                 => Some(1.071) // Newspaper 7.1%
      case Print2026C1NPSemiannualsUK                 => Some(1.071) // Newspaper 7.1%
      case Print2026C2NPMonthliesUK                   => Some(1.071) // Newspaper 7.1%
      case Print2026C3GWMonthliesUK                   => Some(1.10) // GuardianWeekly 10%
      case Print2026C3NPMonthliesUK                   => Some(1.071) // Newspaper 7.1%
      case Print2026C4NPMonthliesUK                   => Some(1.071) // Newspaper 7.1%
      case Print2026C5GWMonthliesAnnualsNoEmailsNonUK => Some(1.10) // GuardianWeekly 10%
      case Print2026C5NPNoEmailsUK                    => Some(1.071) // Newspaper 7.1%
      case Print2026C6GWQuarterliesNonUK              => Some(1.10) // GuardianWeekly 10%
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
