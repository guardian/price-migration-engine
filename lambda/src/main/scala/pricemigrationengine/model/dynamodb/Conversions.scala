package pricemigrationengine.model.dynamodb

import pricemigrationengine.model.CohortTableFilter
import software.amazon.awssdk.services.dynamodb.model.AttributeAction.PUT
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, AttributeValueUpdate}

import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.{ISO_DATE_TIME, ISO_LOCAL_DATE}
import java.time.{Instant, LocalDate}
import java.util
import scala.jdk.CollectionConverters._
import scala.util.Try

object Conversions {

  def stringFieldUpdate(fieldName: String, stringValue: String): (String, AttributeValueUpdate) =
    fieldName -> AttributeValueUpdate.builder.value(AttributeValue.builder.s(stringValue).build()).action(PUT).build()

  def dateFieldUpdate(fieldName: String, dateValue: LocalDate): (String, AttributeValueUpdate) =
    fieldName -> AttributeValueUpdate.builder
      .value(AttributeValue.builder.s(dateValue.format(ISO_LOCAL_DATE)).build())
      .action(PUT)
      .build()

  def instantFieldUpdate(fieldName: String, instant: Instant): (String, AttributeValueUpdate) =
    fieldName -> AttributeValueUpdate.builder
      .value(AttributeValue.builder.s(ISO_DATE_TIME.format(instant.atZone(UTC))).build())
      .action(PUT)
      .build()

  def bigDecimalFieldUpdate(fieldName: String, value: BigDecimal): (String, AttributeValueUpdate) =
    fieldName -> AttributeValueUpdate.builder
      .value(AttributeValue.builder.n(value.toString).build())
      .action(PUT)
      .build()

  def stringUpdate(fieldName: String, stringValue: String): (String, AttributeValue) =
    fieldName -> AttributeValue.builder.s(stringValue).build()

  def getStringFromResults(result: util.Map[String, AttributeValue], fieldName: String): Either[String, String] =
    for {
      optionalString <- getOptionalStringFromResults(result, fieldName)
      string <- optionalString.toRight(s"The '$fieldName' field did not exist in the record '$result''")
    } yield string

  def getDateFromResults(result: util.Map[String, AttributeValue], fieldName: String): Either[String, LocalDate] =
    for {
      string <- getStringFromResults(result, fieldName)
      date <- Try(LocalDate.parse(string)).toEither.left.map(_ =>
        s"The '$fieldName' has value '$string' which is not a valid date yyyy-MM-dd"
      )
    } yield date

  def getCohortTableFilter(
      result: util.Map[String, AttributeValue],
      fieldName: String
  ): Either[String, CohortTableFilter] =
    for {
      string <- getStringFromResults(result, fieldName)
      string <- CohortTableFilter.all
        .find(_.value == string)
        .toRight(s"The '$fieldName' contained an invalid CohortTableFilter '$string'")
    } yield string

  def getOptionalStringFromResults(
      result: util.Map[String, AttributeValue],
      fieldName: String
  ): Either[String, Option[String]] =
    result.asScala
      .get(fieldName)
      .fold[Either[String, Option[String]]](Right(None)) { attributeValue =>
        Option(attributeValue.s)
          .map(Some.apply)
          .toRight(s"The '$fieldName' field was not a string in the record '$result'")
      }

  def getOptionalNumberStringFromResults(
      result: util.Map[String, AttributeValue],
      fieldName: String
  ): Either[String, Option[String]] =
    result.asScala
      .get(fieldName)
      .fold[Either[String, Option[String]]](Right(None)) { attributeValue =>
        Option(attributeValue.n)
          .map(Some.apply)
          .toRight(s"The '$fieldName' field was not a number in the record '$result'")
      }

  def getOptionalDateFromResults(
      result: util.Map[String, AttributeValue],
      fieldName: String
  ): Either[String, Option[LocalDate]] =
    for {
      optionalString <- getOptionalStringFromResults(result, fieldName)
      optionalDate <- optionalString.fold[Either[String, Option[LocalDate]]](Right(None)) { string =>
        Try(Some(LocalDate.parse(string))).toEither.left.map(_ =>
          s"The '$fieldName' has value '$string' which is not a valid date yyyy-MM-dd"
        )
      }
    } yield optionalDate

  def getOptionalBigDecimalFromResults(
      result: util.Map[String, AttributeValue],
      fieldName: String
  ): Either[String, Option[BigDecimal]] =
    for {
      optionalNumberString <- getOptionalNumberStringFromResults(result, fieldName)
      optionalDecimal <- optionalNumberString.fold[Either[String, Option[BigDecimal]]](Right(None)) { string =>
        Try(Some(BigDecimal(string))).toEither.left.map(_ =>
          s"The '$fieldName' has value '$string' which is not a valid number"
        )
      }
    } yield optionalDecimal

  def getOptionalInstantFromResults(
      result: util.Map[String, AttributeValue],
      fieldName: String
  ): Either[String, Option[Instant]] =
    for {
      optionalIsoDateTimeString <- getOptionalStringFromResults(result, fieldName)
      optionalDecimal <- optionalIsoDateTimeString.fold[Either[String, Option[Instant]]](Right(None)) { string =>
        Try(Some(Instant.parse(string))).toEither.left.map(ex =>
          s"The '$fieldName' has value '$string' which is not a valid timestamp: $ex"
        )
      }
    } yield optionalDecimal
}
