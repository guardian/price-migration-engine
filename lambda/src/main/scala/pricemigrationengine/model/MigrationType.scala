package pricemigrationengine.model

sealed trait MigrationType
object GW2024 extends MigrationType
object SupporterPlus2024 extends MigrationType

object MigrationType {
  def apply(cohortSpec: CohortSpec): MigrationType = cohortSpec.cohortName match {
    case "GW2024"            => GW2024
    case "SupporterPlus2024" => SupporterPlus2024
  }
}
