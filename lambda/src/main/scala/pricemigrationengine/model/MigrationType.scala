package pricemigrationengine.model

sealed trait MigrationType
object Default extends MigrationType
object Newspaper2024 extends MigrationType
object GW2024 extends MigrationType
object SupporterPlus2024 extends MigrationType
object SPV1V2E2025 extends MigrationType

object MigrationType {
  def apply(cohortSpec: CohortSpec): MigrationType = cohortSpec.cohortName match {
    case "Newspaper2024"     => Newspaper2024
    case "GW2024"            => GW2024
    case "SupporterPlus2024" => SupporterPlus2024
    case "SPV1V2E2025"       => SPV1V2E2025
    case _                   => Default
  }
}
