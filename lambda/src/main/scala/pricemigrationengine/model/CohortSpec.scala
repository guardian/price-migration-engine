package pricemigrationengine.model

import java.time.LocalDate
import java.util

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import pricemigrationengine.model.dynamodb.Conversions._
import upickle.default.{ReadWriter, macroRW}

/** Specification of a cohort.
  *
  * @param cohortName Name that uniquely identifies a cohort, eg. "Vouchers 2020"
  * @param brazeCampaignName Name of the Braze campaign for this cohort.<br />
  *                          Mapping to environment-specific Braze campaign ID is provided by membership-workflow:<br />
  *                          See https://github.com/guardian/membership-workflow/blob/master/conf/PROD.public.conf#L39
  * @param importStartDate Date on which to start importing data from the source S3 bucket.
  * @param earliestPriceMigrationStartDate Earliest date on which any sub in the cohort can have price migrated.
  *                                        The actual date for any sub will depend on its billing dates.
  * @param migrationCompleteDate Date on which the final step in the price migration was complete for every sub in the cohort.
  *
  * @param tmpTableName A temporary value for the special case where
  *                     the table name can't be derived from the cohort name.
  */
case class CohortSpec(
    cohortName: String,
    brazeCampaignName: String,
    importStartDate: LocalDate,
    earliestPriceMigrationStartDate: LocalDate,
    migrationCompleteDate: Option[LocalDate] = None,
    tmpTableName: Option[String] = None // TODO: remove when price migration 2020 complete
) {
  val normalisedCohortName: String = cohortName.replaceAll(" ", "")
  def tableName(stage: String): String = tmpTableName getOrElse s"PriceMigration-$stage-$normalisedCohortName"
}

object CohortSpec {

  implicit val rw: ReadWriter[CohortSpec] = macroRW

  def isActive(spec: CohortSpec)(date: LocalDate): Boolean =
    !spec.importStartDate.isAfter(date) && spec.migrationCompleteDate.forall(_.isAfter(date))

  def isValid(spec: CohortSpec): Boolean = {
    def isValidStringValue(s: String) = s.trim == s && s.nonEmpty && s.matches("[A-Za-z0-9-_ ]+")
    isValidStringValue(spec.cohortName) &&
    isValidStringValue(spec.brazeCampaignName) &&
    spec.tmpTableName.forall(isValidStringValue) &&
    spec.earliestPriceMigrationStartDate.isAfter(spec.importStartDate)
  }

  def fromDynamoDbItem(values: util.Map[String, AttributeValue]): Either[CohortSpecFetchFailure, CohortSpec] =
    (for {
      cohortName <- getStringFromResults(values, "cohortName")
      brazeCampaignName <- getStringFromResults(values, "brazeCampaignName")
      importStartDate <- getDateFromResults(values, "importStartDate")
      earliestPriceMigrationStartDate <- getDateFromResults(values, "earliestPriceMigrationStartDate")
      migrationCompleteDate <- getOptionalDateFromResults(values, "migrationCompleteDate")
      tmpTableName <- getOptionalStringFromResults(values, "tmpTableName")
    } yield CohortSpec(
      cohortName,
      brazeCampaignName,
      importStartDate,
      earliestPriceMigrationStartDate,
      migrationCompleteDate,
      tmpTableName
    )).left.map(e => CohortSpecFetchFailure(e))
}
