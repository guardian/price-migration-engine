package pricemigrationengine.model

import java.time.LocalDate
class DispatchTest extends munit.FunSuite {

  test("Dispatch (1)") {
    val cohortSpec = CohortSpec(
      cohortName = "DigiSubs2025",
      earliestAmendmentEffectiveDate = LocalDate.of(2026, 2, 1)
    )

    // For DigiSubs2025 we expect the belong function to always return true
    // (Each cohort item belongs to it)

    val cohortItem1: CohortItem = CohortItem(
      subscriptionName = "A-00001",
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
    )
    assertEquals(Dispatch.belongs(cohortSpec, cohortItem1), true)

    val cohortItem2: CohortItem = CohortItem(
      subscriptionName = "A-00004",
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
    )
    assertEquals(Dispatch.belongs(cohortSpec, cohortItem2), true)
  }

  test("Dispatch (2)") {
    val cohortSpec = CohortSpec(
      cohortName = "SupporterPlus2026",
      earliestAmendmentEffectiveDate = LocalDate.of(2026, 8, 1)
    )

    // For SupporterPlus2026,
    // "A-00001" doesn't belong to it (instead "A-00001" belongs to SupporterPlus2026N4, as per next test)
    // "A-00004" belongs to it

    val cohortItem1: CohortItem = CohortItem(
      subscriptionName = "A-00001",
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
    )
    assertEquals(Dispatch.belongs(cohortSpec, cohortItem1), false)

    val cohortItem2: CohortItem = CohortItem(
      subscriptionName = "A-00004",
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
    )
    assertEquals(Dispatch.belongs(cohortSpec, cohortItem2), true)
  }

  test("Dispatch (3)") {
    // "A-00001" belongs to SupporterPlus2026N4

    val cohortSpec = CohortSpec(
      cohortName = "SupporterPlus2026N4",
      earliestAmendmentEffectiveDate = LocalDate.of(2026, 8, 1)
    )

    val cohortItem1: CohortItem = CohortItem(
      subscriptionName = "A-00001",
      processingStage = CohortTableFilter.NotificationSendDateWrittenToSalesforce,
    )
    assertEquals(Dispatch.belongs(cohortSpec, cohortItem1), true)
  }
}
