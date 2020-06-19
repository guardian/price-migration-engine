package pricemigrationengine.model

import java.time.LocalDate

class CohortSpecTest extends munit.FunSuite {

  private val importStartDate = LocalDate.of(2020, 5, 1)
  private val migrationCompleteDate = LocalDate.of(2020, 6, 18)

  private def isActive(onDay: LocalDate, migrationComplete: Option[LocalDate] = Some(migrationCompleteDate)) =
    CohortSpec.isActive(
      CohortSpec(
        "name",
        LocalDate.of(2021, 1, 1),
        importStartDate,
        migrationComplete
      ))(onDay)

  test("isActive: should be false when given date is before import start date") {
    assertEquals(isActive(importStartDate.minusDays(3)), false)
  }

  test("isActive: should be true when given date is import start date") {
    assertEquals(isActive(importStartDate), true)
  }

  test("isActive: should be true when given date is between import start date and migration complete date") {
    assertEquals(isActive(importStartDate.plusDays(3)), true)
  }

  test("isActive: should be false when given date is on migration complete date") {
    assertEquals(isActive(migrationCompleteDate), false)
  }

  test("isActive: should be false when given date is after migration complete date") {
    assertEquals(isActive(migrationCompleteDate.plusDays(3)), false)
  }

  test("isActive: should be true when given date is after import start date and migration isn't complete") {
    assertEquals(isActive(importStartDate.plusDays(3), migrationComplete = None), true)
  }
}
