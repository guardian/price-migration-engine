package pricemigrationengine.model

import pricemigrationengine.model.CohortTableFilter
import pricemigrationengine.model.Estimation1

import java.time.{Instant, LocalDate}

class EstimationTest extends munit.FunSuite {
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
  test("Estimation.isProcessable (with None)") {
    val item = CohortItem(
      subscriptionName = "subscriptionName",
      processingStage = CohortTableFilter.ReadyForEstimation,
      doNotProcessUntil = None
    )
    val today = LocalDate.of(2024, 9, 9)
    assertEquals(
      Estimation1.isProcessable(item, today),
      true
    )
  }
  test("Estimation.isProcessable (today before date)") {
    val item = CohortItem(
      subscriptionName = "subscriptionName",
      processingStage = CohortTableFilter.ReadyForEstimation,
      doNotProcessUntil = Some(LocalDate.of(2024, 9, 10))
    )
    val today = LocalDate.of(2024, 9, 9)
    assertEquals(
      Estimation1.isProcessable(item, today),
      false
    )
  }
  test("Estimation.isProcessable (today equals date)") {
    val item = CohortItem(
      subscriptionName = "subscriptionName",
      processingStage = CohortTableFilter.ReadyForEstimation,
      doNotProcessUntil = Some(LocalDate.of(2024, 9, 10))
    )
    val today = LocalDate.of(2024, 9, 10)
    assertEquals(
      Estimation1.isProcessable(item, today),
      true
    )
  }
  test("Estimation.isProcessable (today after date)") {
    val item = CohortItem(
      subscriptionName = "subscriptionName",
      processingStage = CohortTableFilter.ReadyForEstimation,
      doNotProcessUntil = Some(LocalDate.of(2024, 9, 10))
    )
    val today = LocalDate.of(2024, 9, 11)
    assertEquals(
      Estimation1.isProcessable(item, today),
      true
    )
  }
}
