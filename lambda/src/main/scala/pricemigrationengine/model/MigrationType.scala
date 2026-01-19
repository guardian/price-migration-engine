package pricemigrationengine.model

sealed trait MigrationType
object Test1 extends MigrationType // This is the Migration Type to use in tests, when a CohortSpec is needed
object GuardianWeekly2025 extends MigrationType
object Newspaper2025P1 extends MigrationType
object Newspaper2025P3 extends MigrationType
object ProductMigration2025N4 extends MigrationType
object Membership2025 extends MigrationType
object DigiSubs2025 extends MigrationType

object MigrationType {
  def apply(cohortSpec: CohortSpec): MigrationType = cohortSpec.cohortName match {
    case "Test1"                  => Test1
    case "GuardianWeekly2025"     => GuardianWeekly2025
    case "Newspaper2025P1"        => Newspaper2025P1
    case "Newspaper2025P3"        => Newspaper2025P3
    case "ProductMigration2025N4" => ProductMigration2025N4
    case "Membership2025"         => Membership2025
    case "DigiSubs2025"           => DigiSubs2025
  }
}
