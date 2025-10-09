package pricemigrationengine.model

import java.time.{Instant, LocalDate}

class CohortItemTest extends munit.FunSuite {
  test("calibration (1)") {
    assertEquals(
      LocalDate.of(2024, 9, 9) == LocalDate.of(2024, 9, 9),
      true
    )
  }
  test("calibration (2)") {
    // This tests shows that .isAfter is strict
    assertEquals(
      LocalDate.of(2024, 9, 9).isAfter(LocalDate.of(2024, 9, 9)),
      false
    )
    assertEquals(
      LocalDate.of(2024, 9, 10).isAfter(LocalDate.of(2024, 9, 9)),
      true
    )
  }
  test("CohortItem.isProcessable (DoNotProcessUntil, with no date set)") {
    val item = CohortItem(
      subscriptionName = "subscriptionName",
      processingStage = CohortTableFilter.DoNotProcessUntil,
      doNotProcessUntil = None
    )
    val today = LocalDate.of(2024, 9, 9)
    val exception = intercept[Exception] {
      ItemHibernation.isProcessable(item, today)
    }
    assertEquals(exception.getMessage.contains("(error: 588b7698)"), true)
  }
  test("CohortItem.isProcessable (DoNotProcessUntil, today before date)") {
    val item = CohortItem(
      subscriptionName = "subscriptionName",
      processingStage = CohortTableFilter.DoNotProcessUntil,
      doNotProcessUntil = Some(LocalDate.of(2024, 9, 10))
    )
    val today = LocalDate.of(2024, 9, 9)
    assertEquals(
      ItemHibernation.isProcessable(item, today),
      false
    )
  }
  test("CohortItem.isProcessable (DoNotProcessUntil, today equals date)") {
    val item = CohortItem(
      subscriptionName = "subscriptionName",
      processingStage = CohortTableFilter.DoNotProcessUntil,
      doNotProcessUntil = Some(LocalDate.of(2024, 9, 10))
    )
    val today = LocalDate.of(2024, 9, 10)
    assertEquals(
      ItemHibernation.isProcessable(item, today),
      true
    )
  }
  test("CohortItem.isProcessable (DoNotProcessUntil, today after date)") {
    val item = CohortItem(
      subscriptionName = "subscriptionName",
      processingStage = CohortTableFilter.DoNotProcessUntil,
      doNotProcessUntil = Some(LocalDate.of(2024, 9, 10))
    )
    val today = LocalDate.of(2024, 9, 11)
    assertEquals(
      ItemHibernation.isProcessable(item, today),
      true
    )
  }
}
