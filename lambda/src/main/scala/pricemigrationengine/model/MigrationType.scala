package pricemigrationengine.model

sealed trait MigrationType
object Default extends MigrationType
object DigiSubs2023 extends MigrationType
object Newspaper2024 extends MigrationType
object GW2024 extends MigrationType
object SupporterPlus2024 extends MigrationType

object MigrationType {
  def apply(cohortSpec: CohortSpec): MigrationType = cohortSpec.cohortName match {
    case "DigiSubs2023_Batch1" => DigiSubs2023
    case "DigiSubs2023_Batch2" => DigiSubs2023
    case "Newspaper2024"       => Newspaper2024
    case "GW2024"              => GW2024
    case "SupporterPlus2024"   => SupporterPlus2024
    case _                     => Default
  }
}
