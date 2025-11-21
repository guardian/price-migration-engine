package pricemigrationengine.model

import java.time.LocalDate

class DateTest extends munit.FunSuite {

  test("Date.equalOrInOrder (1)") {
    assertEquals(
      Date.equalOrInOrder(
        LocalDate.of(2025, 11, 20),
        LocalDate.of(2025, 11, 19)
      ),
      false
    )
  }

  test("Date.equalOrInOrder (2)") {
    assertEquals(
      Date.equalOrInOrder(
        LocalDate.of(2025, 11, 20),
        LocalDate.of(2025, 11, 20)
      ),
      true
    )
  }

  test("Date.equalOrInOrder (3)") {
    assertEquals(
      Date.equalOrInOrder(
        LocalDate.of(2025, 11, 20),
        LocalDate.of(2025, 11, 21)
      ),
      true
    )
  }
}
