package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.migrations.SupporterPlus2024Migration

class SupporterPlus2024MigrationTest extends munit.FunSuite {
  test("isInCancellationSave") {
    val subscriptionYes =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-yes.json")
    val subscriptionNo =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-no.json")
    assertEquals(
      SupporterPlus2024Migration.isInCancellationSave(subscriptionNo),
      false
    )
    assertEquals(
      SupporterPlus2024Migration.isInCancellationSave(subscriptionYes),
      true
    )
  }
  test("cancellationSaveEffectiveDate") {
    val subscriptionYes =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-yes.json")
    val subscriptionNo =
      Fixtures.subscriptionFromJson("Migrations/SupporterPlus2024/sub-with-cancellation-save/subscription-no.json")
    assertEquals(
      SupporterPlus2024Migration.cancellationSaveEffectiveDate(subscriptionNo),
      None
    )
    assertEquals(
      SupporterPlus2024Migration.cancellationSaveEffectiveDate(subscriptionYes),
      Some(LocalDate.of(2024, 7, 5))
    )
  }

  test("Price Grid") {
    assertEquals(
      SupporterPlus2024Migration.getPrice(Monthly, "USD"),
      Some(15.0)
    )
  }

}
