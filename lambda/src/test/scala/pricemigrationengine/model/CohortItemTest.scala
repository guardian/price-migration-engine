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
}
