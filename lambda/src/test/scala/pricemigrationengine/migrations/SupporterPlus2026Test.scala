package pricemigrationengine.migrations

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.Fixtures

import java.time.LocalDate

class SupporterPlus2026Test extends munit.FunSuite {
  test("date calculation") {
    assertEquals(
      SupporterPlus2026Migration.monthliesOverSixWeeks(LocalDate.of(2026, 7, 20), Annual),
      LocalDate.of(2026, 7, 20)
    )
    assertEquals(
      SupporterPlus2026Migration.monthliesOverSixWeeks(LocalDate.of(2026, 9, 1), Monthly),
      LocalDate.of(2026, 9, 1)
    )
    assert(
      List(LocalDate.of(2026, 7, 21), LocalDate.of(2026, 8, 21))
        .contains(SupporterPlus2026Migration.monthliesOverSixWeeks(LocalDate.of(2026, 7, 21), Monthly))
    )
  }
}
