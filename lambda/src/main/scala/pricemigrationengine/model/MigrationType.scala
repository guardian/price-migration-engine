package pricemigrationengine.model

sealed trait MigrationType
object SupporterPlus2024 extends MigrationType
object GuardianWeekly2025 extends MigrationType
object Newspaper2025P1 extends MigrationType

object MigrationType {
  def apply(cohortSpec: CohortSpec): MigrationType = cohortSpec.cohortName match {
    case "SupporterPlus2024"  => SupporterPlus2024
    case "GuardianWeekly2025" => GuardianWeekly2025
    case "Newspaper2025P1"    => Newspaper2025P1
  }
}
