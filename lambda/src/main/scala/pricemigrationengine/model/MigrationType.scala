package pricemigrationengine.model

/*
  MigrationType.apply: CohortSpec -> MigrationType
  was introduced to help remove the `if else if else if ... else` pattern that was showing up as we started to
  have more migrations, notably the SupporterPlus 2023 migration after the membership annuals. Having defined a
  sealed trait means that we can use a `match / case` layout, which makes the code more readable

  MigrationType does not identity a migration (despite the fact that some migrations map to a unique migration type)
  It simply helps identify common code used by possibly more than one migration. For instance all the pre 2023 migrations
  map to `Legacy`
 */

sealed trait MigrationType
object Legacy extends MigrationType // refers to all migrations before membership 2023 and supporter 2023
object Membership2023Monthlies extends MigrationType
object Membership2023Annuals extends MigrationType
object SupporterPlus2023V1V2MA extends MigrationType
object DigiSubs2023 extends MigrationType
object Newspaper2024 extends MigrationType
object GW2024 extends MigrationType
object SupporterPlus2024 extends MigrationType

object MigrationType {
  def apply(cohortSpec: CohortSpec): MigrationType = cohortSpec.cohortName match {
    case "Membership2023_Batch1" => Membership2023Monthlies
    case "Membership2023_Batch2" => Membership2023Monthlies
    case "Membership2023_Batch3" => Membership2023Annuals
    case "SupporterPlus2023V1V2" => SupporterPlus2023V1V2MA
    case "DigiSubs2023_Batch1"   => DigiSubs2023
    case "DigiSubs2023_Batch2"   => DigiSubs2023
    case "Newspaper2024"         => Newspaper2024
    case "GW2024"                => GW2024
    case _                       => Legacy
  }
}
