package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.handlers.NotificationHandler
import pricemigrationengine.migrations.SupporterPlus2024Migration
import pricemigrationengine.util.StartDates

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
      Some(LocalDate.of(2024, 6, 3))
    )
  }
}
