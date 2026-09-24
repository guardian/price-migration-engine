package pricemigrationengine.model

import pricemigrationengine.Fixtures

import java.time.LocalDate

class GuardianWeeklyHelperTest extends munit.FunSuite {

  test("GuardianWeeklyHelper.subscriptionToFinancePercentageDistribution") {

    assertEquals(
      GuardianWeeklyHelper.subscriptionToFinancePercentageDistribution(Monthly, "USD", Domestic),
      Some(
        T5xDistribution(
          guardianWeeklyPercentage = BigDecimal(65.1),
          digitalPackPercentage = BigDecimal(34.9)
        )
      )
    )
  }
}
