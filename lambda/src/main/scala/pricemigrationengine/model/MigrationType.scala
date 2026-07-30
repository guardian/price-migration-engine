package pricemigrationengine.model

sealed trait MigrationType
object Test1 extends MigrationType // This is the Migration Type to use in tests, when a CohortSpec is needed
object GuardianWeekly2025 extends MigrationType
object Newspaper2025P1 extends MigrationType
object Newspaper2025P3 extends MigrationType
object ProductMigration2025N4 extends MigrationType
object Membership2025 extends MigrationType
object DigiSubs2025 extends MigrationType
object SupporterPlus2026 extends MigrationType
object SupporterPlus2026N2 extends MigrationType
object SupporterPlus2026N3 extends MigrationType
object SupporterPlus2026N4 extends MigrationType
object SupporterPlus2026N5 extends MigrationType

object MigrationType {
  def apply(cohortSpec: CohortSpec): MigrationType = cohortSpec.cohortName match {
    case "Test1"                  => Test1
    case "GuardianWeekly2025"     => GuardianWeekly2025
    case "Newspaper2025P1"        => Newspaper2025P1
    case "Newspaper2025P3"        => Newspaper2025P3
    case "ProductMigration2025N4" => ProductMigration2025N4
    case "Membership2025"         => Membership2025
    case "DigiSubs2025"           => DigiSubs2025
    case "SupporterPlus2026"      => SupporterPlus2026
    case "SupporterPlus2026N2"    => SupporterPlus2026N2
    case "SupporterPlus2026N3"    => SupporterPlus2026N3
    case "SupporterPlus2026N4"    => SupporterPlus2026N4
    case "SupporterPlus2026N5"    => SupporterPlus2026N5
  }
}
