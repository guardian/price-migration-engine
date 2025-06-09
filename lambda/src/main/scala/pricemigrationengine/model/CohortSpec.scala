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
  * @param brazeName
  *   Name of the Braze campaign, or Braze canvas for this cohort.
  *   Mapping to environment-specific Braze campaign ID is provided by membership-workflow:
  *   See https://github.com/guardian/membership-workflow/blob/master/conf/PROD.public.conf#L39
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
    brazeName: String,
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
    isValidStringValue(spec.brazeName) &&
    spec.earliestPriceMigrationStartDate.isAfter(spec.importStartDate)
  }

  def fromDynamoDbItem(values: util.Map[String, AttributeValue]): Either[CohortSpecFetchFailure, CohortSpec] =
    (for {
      cohortName <- getStringFromResults(values, "cohortName")
      brazeName <- getStringFromResults(values, "brazeName")
      importStartDate <- getDateFromResults(values, "importStartDate")
      earliestPriceMigrationStartDate <- getDateFromResults(values, "earliestPriceMigrationStartDate")
      migrationCompleteDate <- getOptionalDateFromResults(values, "migrationCompleteDate")
    } yield CohortSpec(
      cohortName,
      brazeName,
      importStartDate,
      earliestPriceMigrationStartDate,
      migrationCompleteDate
    )).left.map(e => CohortSpecFetchFailure(e))
}
