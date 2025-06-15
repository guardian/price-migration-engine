package pricemigrationengine.libs

import pricemigrationengine.Fixtures
import pricemigrationengine.model._
import java.time.LocalDate

// Subscription 1 is standard, USD is an address in the `United States`
// val subscription = Fixtures.subscriptionFromJson("libs/SubscriptionLocalisation/subscription1/subscription.json")
// val account = Fixtures.accountFromJson("libs/SubscriptionLocalisation/subscription1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("libs/SubscriptionLocalisation/subscription1/invoice-preview.json")

// Subscription 2 is ROW (USD variant), USD is an address in the `France`
// val subscription = Fixtures.subscriptionFromJson("libs/SubscriptionLocalisation/subscription2/subscription.json")
// val account = Fixtures.accountFromJson("libs/SubscriptionLocalisation/subscription2/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("libs/SubscriptionLocalisation/subscription2/invoice-preview.json")

// Subscription 3 is ROW (GBP variant), USD is an address in the `France`
// val subscription = Fixtures.subscriptionFromJson("libs/SubscriptionLocalisation/subscription3/subscription.json")
// val account = Fixtures.accountFromJson("libs/SubscriptionLocalisation/subscription3/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("libs/SubscriptionLocalisation/subscription3/invoice-preview.json")

class SubscriptionLocalisationTest extends munit.FunSuite {
  test("determineSubscriptionLocalisation (1): Domestic") {
    val subscription =
      Fixtures.subscriptionFromJson("libs/SubscriptionLocalisation/subscription1/subscription.json")
    val account = Fixtures.accountFromJson("libs/SubscriptionLocalisation/subscription1/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("libs/SubscriptionLocalisation/subscription1/invoice-preview.json")
    val localization = SubscriptionLocalisation.determineSubscriptionLocalisation(
      subscription,
      invoicePreview,
      account
    )
    // In this case, we have a USD subscription in the US, so we get Domestic
    assertEquals(localization, Some(Domestic))
  }

  test("determineSubscriptionLocalisation (2): ROW (USD Variant, France address)") {
    val subscription =
      Fixtures.subscriptionFromJson("libs/SubscriptionLocalisation/subscription2/subscription.json")
    val account = Fixtures.accountFromJson("libs/SubscriptionLocalisation/subscription2/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("libs/SubscriptionLocalisation/subscription2/invoice-preview.json")
    val localization = SubscriptionLocalisation.determineSubscriptionLocalisation(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(subscription.ratePlans.headOption.get.ratePlanCharges.headOption.get.currency, "USD")
    assertEquals(account.soldToContact.country, "France")

    // ROW (USD Variant, France address)
    assertEquals(localization, Some(RestOfWorld))
  }

  test("determineSubscriptionLocalisation (3): ROW (GBP Variant, France address)") {
    val subscription =
      Fixtures.subscriptionFromJson("libs/SubscriptionLocalisation/subscription3/subscription.json")
    val account = Fixtures.accountFromJson("libs/SubscriptionLocalisation/subscription3/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("libs/SubscriptionLocalisation/subscription3/invoice-preview.json")
    val localization = SubscriptionLocalisation.determineSubscriptionLocalisation(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(subscription.ratePlans.headOption.get.ratePlanCharges.headOption.get.currency, "GBP")
    assertEquals(account.soldToContact.country, "France")

    // ROW (GBP Variant, France address)
    assertEquals(localization, Some(RestOfWorld))
  }
}
