package pricemigrationengine.model

import pricemigrationengine.model.dynamodb.Conversions._
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import upickle.default.{ReadWriter, macroRW}

import java.time.LocalDate
import java.util

/** Specification of a cohort.
  *
  * @param cohortName
  *   Name that uniquely identifies a cohort, eg. "Vouchers 2020"
  * @param brazeCampaignName
  *   Name of the Braze campaign for this cohort.<br /> Mapping to environment-specific Braze campaign ID is provided by
  *   membership-workflow:<br /> See
  *   https://github.com/guardian/membership-workflow/blob/master/conf/PROD.public.conf#L39
  * @param importStartDate
  *   Date on which to start importing data from the source S3 bucket.
  * @param earliestPriceMigrationStartDate
  *   Earliest date on which any sub in the cohort can have price migrated. The actual date for any sub will depend on
  *   its billing dates.
  * @param migrationCompleteDate
  *   Date on which the final step in the price migration was complete for every sub in the cohort.
  */
case class CohortSpec(
    cohortName: String,
    brazeCampaignName: String,
    importStartDate: LocalDate,
    earliestPriceMigrationStartDate: LocalDate,
    migrationCompleteDate: Option[LocalDate] = None
) {
  val normalisedCohortName: String = cohortName.replaceAll(" ", "")
  def tableName(stage: String): String = s"PriceMigration-$stage-$normalisedCohortName"
}

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
object SupporterPlus2023V1V2 extends MigrationType

object MigrationType {
  def apply(cohortSpec: CohortSpec): MigrationType = cohortSpec.cohortName match {
    case "Membership2023_Batch1"    => Membership2023Monthlies
    case "Membership2023_Batch2"    => Membership2023Monthlies
    case "Membership2023_Batch3"    => Membership2023Annuals
    case "SupporterRevenue2023V1V2" => SupporterPlus2023V1V2
    case _                          => Legacy
  }
}

object CohortSpec {

  implicit val rw: ReadWriter[CohortSpec] = macroRW

  def isActive(spec: CohortSpec)(date: LocalDate): Boolean =
    !spec.importStartDate.isAfter(date) && spec.migrationCompleteDate.forall(_.isAfter(date))

  def isValid(spec: CohortSpec): Boolean = {
    def isValidStringValue(s: String) = s.trim == s && s.nonEmpty && s.matches("[A-Za-z0-9-_ ]+")
    isValidStringValue(spec.cohortName) &&
    isValidStringValue(spec.brazeCampaignName) &&
    spec.earliestPriceMigrationStartDate.isAfter(spec.importStartDate)
  }

  def fromDynamoDbItem(values: util.Map[String, AttributeValue]): Either[CohortSpecFetchFailure, CohortSpec] =
    (for {
      cohortName <- getStringFromResults(values, "cohortName")
      brazeCampaignName <- getStringFromResults(values, "brazeCampaignName")
      importStartDate <- getDateFromResults(values, "importStartDate")
      earliestPriceMigrationStartDate <- getDateFromResults(values, "earliestPriceMigrationStartDate")
      migrationCompleteDate <- getOptionalDateFromResults(values, "migrationCompleteDate")
    } yield CohortSpec(
      cohortName,
      brazeCampaignName,
      importStartDate,
      earliestPriceMigrationStartDate,
      migrationCompleteDate
    )).left.map(e => CohortSpecFetchFailure(e))
}
