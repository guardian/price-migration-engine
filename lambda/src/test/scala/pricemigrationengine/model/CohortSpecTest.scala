package pricemigrationengine.model

import java.time.LocalDate

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import pricemigrationengine.model.CohortSpec.isValid

import scala.jdk.CollectionConverters._

class CohortSpecTest extends munit.FunSuite {

  private val importStartDate = LocalDate.of(2020, 5, 1)
  private val migrationCompleteDate = LocalDate.of(2020, 6, 18)

  private val cohortSpec = CohortSpec(
    cohortName = "Home Delivery 2018",
    brazeCampaignName = "cmp123",
    importStartDate = LocalDate.of(2020, 1, 1),
    earliestPriceMigrationStartDate = LocalDate.of(2020, 1, 2),
    migrationCompleteDate = None,
    tmpTableName = None
  )

  private def assertTrue(obtained: Boolean): Unit = assertEquals(obtained, true)
  private def assertFalse(obtained: Boolean): Unit = assertEquals(obtained, false)

  private def isActive(onDay: LocalDate, migrationComplete: Option[LocalDate] = Some(migrationCompleteDate)) =
    CohortSpec.isActive(
      CohortSpec(
        cohortName = "name",
        brazeCampaignName = "cmp123",
        importStartDate,
        earliestPriceMigrationStartDate = LocalDate.of(2021, 1, 1),
        migrationComplete
      )
    )(onDay)

  test("isActive: should be false when given date is before import start date") {
    assertFalse(isActive(importStartDate.minusDays(3)))
  }

  test("isActive: should be true when given date is import start date") {
    assertTrue(isActive(importStartDate))
  }

  test("isActive: should be true when given date is between import start date and migration complete date") {
    assertTrue(isActive(importStartDate.plusDays(3)))
  }

  test("isActive: should be false when given date is on migration complete date") {
    assertFalse(isActive(migrationCompleteDate))
  }

  test("isActive: should be false when given date is after migration complete date") {
    assertFalse(isActive(migrationCompleteDate.plusDays(3)))
  }

  test("isActive: should be true when given date is after import start date and migration isn't complete") {
    assertTrue(isActive(importStartDate.plusDays(3), migrationComplete = None))
  }

  test("tableName: should be tmpTableName field value when present") {
    assertEquals(
      cohortSpec.copy(tmpTableName = Some("givenName")).tableName(stage = "DEV"),
      "givenName"
    )
  }

  test("tableName: should be transformed cohort name when tmpTableName field value not present") {
    assertEquals(
      cohortSpec.tableName(stage = "PROD"),
      "PriceMigration-PROD-HomeDelivery2018"
    )
  }

  test("fromDynamoDbItem: should include all fields") {
    val item = Map(
      "cohortName" -> new AttributeValue().withS("Home Delivery 2018"),
      "brazeCampaignName" -> new AttributeValue().withS("cmp123"),
      "importStartDate" -> new AttributeValue().withS("2020-01-01"),
      "earliestPriceMigrationStartDate" -> new AttributeValue().withS("2020-01-02"),
      "tmpTableName" -> new AttributeValue().withS("tmpName")
    ).asJava
    assertEquals(
      CohortSpec.fromDynamoDbItem(item),
      Right(cohortSpec.copy(tmpTableName = Some("tmpName")))
    )
  }

  test("isValid: should be true when all fields are valid") {
    assertTrue(isValid(cohortSpec))
  }

  test("isValid: should be false when the cohort name has trailing whitespace") {
    assertFalse(isValid(cohortSpec.copy(cohortName = "Home Delivery 2018 ")))
  }

  test("isValid: should be false when the campaign name contains an illegal character") {
    assertFalse(isValid(cohortSpec.copy(brazeCampaignName = "vc:ppr321")))
  }

  test("isValid: should be false when the import date is not before the earliest migration start date") {
    assertFalse(isValid(cohortSpec.copy(earliestPriceMigrationStartDate = LocalDate.of(2020, 1, 1))))
  }
}
