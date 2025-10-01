package pricemigrationengine.model

import pricemigrationengine.model.dynamodb.Conversions._
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import upickle.default.{ReadWriter, macroRW}

import java.time.LocalDate
import java.util

/** Specification of a cohort.
  *
  * @param cohortName
  *   Name that uniquely identifies a cohort, eg. "Vouchers2020"
  *
  * @param brazeName
  *   Name of the Braze campaign, or Braze canvas for this cohort.
  *   Mapping to environment-specific Braze campaign ID is provided by membership-workflow:
  *   See https://github.com/guardian/membership-workflow/blob/master/conf/PROD.public.conf#L39
  *
  * @param earliestAmendmentEffectDate
  *   Earliest date on which any sub in the cohort can have price migrated. The actual date for any sub will depend on
  *   its billing dates.
  *
  * @param subscriptionNumber
  *   subscriptionNumber is an optional attribute, which, when present, and in the correct context (eg:
  *   estimation handler, notification handler and amendment handler) is going to cause the handler to run
  *   on this one specified subscription, rather than a subset of the cohort table. Note that for security,
  *   the handler must still verify that the item, if found, has the right processing stage for this particular
  *   handler. This attribute was added to CohortSpec in August 2025, to help with rescuing some print migration
  *   cohort items causing timeouts in Zuora. This attribute is not intended to be used by the state machine.
  *
  * @param forceNotifications
  *   When present, and if true, will override the Notification
  *   handler's `thereIsEnoughNotificationLeadTime` check. This allows sending notifications
  *   before the -30 days deadline, but after they have exited their migration notification window
  */
case class CohortSpec(
    cohortName: String,
    brazeName: String,
    earliestAmendmentEffectDate: LocalDate,
    subscriptionNumber: Option[String] = None,
    forceNotifications: Option[Boolean] = None,
) {
  def tableName(stage: String): String = s"PriceMigration-${stage}-${cohortName}"
}

object CohortSpec {

  implicit val rw: ReadWriter[CohortSpec] = macroRW

  def isValid(spec: CohortSpec): Boolean = {
    def isValidStringValue(s: String) = s.trim == s && s.nonEmpty && s.matches("[A-Za-z0-9-_ ]+")
    isValidStringValue(spec.cohortName) &&
    isValidStringValue(spec.brazeName)
  }

  def fromDynamoDbItem(values: util.Map[String, AttributeValue]): Either[CohortSpecFetchFailure, CohortSpec] =
    (for {
      cohortName <- getStringFromResults(values, "cohortName")
      brazeName <- getStringFromResults(values, "brazeName")
      earliestAmendmentEffectDate <- getDateFromResults(values, "earliestAmendmentEffectDate")
    } yield CohortSpec(
      cohortName,
      brazeName,
      earliestAmendmentEffectDate
    )).left.map(e => CohortSpecFetchFailure(e))
}
