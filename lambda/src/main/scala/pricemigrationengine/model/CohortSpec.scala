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

  def isMembershipPriceRiseBatch1(cohortSpec: CohortSpec): Boolean = cohortSpec.cohortName == "Membership2023_Batch1"
  def isMembershipPriceRiseBatch2(cohortSpec: CohortSpec): Boolean = cohortSpec.cohortName == "Membership2023_Batch2"
  def isMembershipPriceRiseBatch3(cohortSpec: CohortSpec): Boolean = cohortSpec.cohortName == "Membership2023_Batch3"

  def isMembershipPriceRiseMonthlies(cohortSpec: CohortSpec) =
    isMembershipPriceRiseBatch1(cohortSpec) || isMembershipPriceRiseBatch2(cohortSpec)

  def isMembershipPriceRiseAnnuals(cohortSpec: CohortSpec) = isMembershipPriceRiseBatch3(cohortSpec)

  def isMembershipPriceRise(cohortSpec: CohortSpec) =
    isMembershipPriceRiseMonthlies(cohortSpec) || isMembershipPriceRiseAnnuals(cohortSpec)
}
