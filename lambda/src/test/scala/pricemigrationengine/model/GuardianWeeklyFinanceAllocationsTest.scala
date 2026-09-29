package pricemigrationengine.model

class GuardianWeeklyFinanceAllocationsTest extends munit.FunSuite {
  test("getDistribution") {
    assertEquals(
      GuardianWeeklyFinanceAllocations.getDistribution(T4xAnnual, "GBP", RestOfWorld),
      Some(T5xFinanceAllocation(BigDecimal(62.8), BigDecimal(37.2)))
    )
  }
  test("getDistribution") {
    assertEquals(
      GuardianWeeklyFinanceAllocations.getDistribution(T4xMonth, "USD", Domestic),
      Some(T5xFinanceAllocation(BigDecimal(65.1), BigDecimal(34.9)))
    )
    assertEquals(
      GuardianWeeklyFinanceAllocations.getDistribution(T4xQuarter, "CAD", Domestic),
      Some(T5xFinanceAllocation(BigDecimal(60.9), BigDecimal(39.1)))
    )
    assertEquals(
      GuardianWeeklyFinanceAllocations.getDistribution(T4xAnnual, "CAD", Domestic),
      Some(T5xFinanceAllocation(BigDecimal(65.1), BigDecimal(34.9)))
    )
    assertEquals(
      GuardianWeeklyFinanceAllocations.getDistribution(T4xSixForSix, "CAD", Domestic),
      Some(T5xFinanceAllocation(BigDecimal(60.9), BigDecimal(39.1)))
    )
  }
  test("T5xFinanceAllocation sum to 100%") {
    // This check ensures that in all cases the two components of T5xFinanceAllocation sum to 100

    assertEquals(
      GuardianWeeklyFinanceAllocations.monthDistributions.values.forall(v =>
        (v.guardianWeeklyPercentage + v.digitalPackPercentage) == BigDecimal(100)
      ),
      true
    )

    assertEquals(
      GuardianWeeklyFinanceAllocations.quarterlyDistributions.values.forall(v =>
        (v.guardianWeeklyPercentage + v.digitalPackPercentage) == BigDecimal(100)
      ),
      true
    )

    assertEquals(
      GuardianWeeklyFinanceAllocations.semiAnnualDistributions.values.forall(v =>
        (v.guardianWeeklyPercentage + v.digitalPackPercentage) == BigDecimal(100)
      ),
      true
    )

    assertEquals(
      GuardianWeeklyFinanceAllocations.annualDistributions.values.forall(v =>
        (v.guardianWeeklyPercentage + v.digitalPackPercentage) == BigDecimal(100)
      ),
      true
    )

    assertEquals(
      GuardianWeeklyFinanceAllocations.sixForSixDistributions.values.forall(v =>
        (v.guardianWeeklyPercentage + v.digitalPackPercentage) == BigDecimal(100)
      ),
      true
    )
  }
}
