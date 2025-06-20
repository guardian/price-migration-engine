package pricemigrationengine.model

sealed trait MigrationType
object Test1 extends MigrationType // This is the Migration Type to use in tests, when a CohortSpec is needed
object SupporterPlus2024 extends MigrationType
object GuardianWeekly2025 extends MigrationType
object Newspaper2025P1 extends MigrationType

object MigrationType {
  def apply(cohortSpec: CohortSpec): MigrationType = cohortSpec.cohortName match {
    case "Test1"              => Test1
    case "SupporterPlus2024"  => SupporterPlus2024
    case "GuardianWeekly2025" => GuardianWeekly2025
    case "Newspaper2025P1"    => Newspaper2025P1
  }
}
