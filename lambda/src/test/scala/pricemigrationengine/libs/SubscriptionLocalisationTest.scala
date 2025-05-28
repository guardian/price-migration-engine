package pricemigrationengine.libs

import pricemigrationengine.Fixtures
import pricemigrationengine.model._
import java.time.LocalDate

// This module reuse the fixtures we introduced for SubscriptionIntrospection2025
// located here: test/resources/libs/SubscriptionIntrospection2025/

// val subscription = Fixtures.subscriptionFromJson("libs/SubscriptionIntrospection2025/subscription1/subscription.json")
// val account = Fixtures.accountFromJson("libs/SubscriptionIntrospection2025/subscription1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("libs/SubscriptionIntrospection2025/subscription1/invoice-preview.json")
// val catalogue = Fixtures.productCatalogueFromJson("libs/SubscriptionIntrospection2025/subscription1/catalogue.json")

class SubscriptionLocalisationTest extends munit.FunSuite {
  test("determineSubscriptionLocalisation") {
    val subscription =
      Fixtures.subscriptionFromJson("libs/SubscriptionIntrospection2025/subscription1/subscription.json")
    val account = Fixtures.accountFromJson("libs/SubscriptionIntrospection2025/subscription1/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("libs/SubscriptionIntrospection2025/subscription1/invoice-preview.json")
    val localization = SubscriptionLocalisation.determineSubscriptionLocalisation(
      subscription,
      invoicePreview,
      account
    )
    // In this case, we have a USD subscription in the US, so we get Domestic
    assertEquals(localization, Some(Domestic))
  }
}
