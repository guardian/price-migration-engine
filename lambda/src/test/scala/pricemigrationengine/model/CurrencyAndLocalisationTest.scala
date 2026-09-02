package pricemigrationengine.model

import pricemigrationengine.Fixtures

// Subscription 1 is standard, USD with an address in the `United States`
// val subscription = Fixtures.subscriptionFromJson("model/SubscriptionLocalisation/subscription1/subscription.json")
// val account = Fixtures.accountFromJson("model/SubscriptionLocalisation/subscription1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("model/SubscriptionLocalisation/subscription1/invoice-preview.json")

// Subscription 2 is ROW (USD variant), USD with an address in `France`
// val subscription = Fixtures.subscriptionFromJson("model/SubscriptionLocalisation/subscription2/subscription.json")
// val account = Fixtures.accountFromJson("model/SubscriptionLocalisation/subscription2/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("model/SubscriptionLocalisation/subscription2/invoice-preview.json")

// Subscription 3 is ROW (GBP variant), GBP with an address in `France`
// val subscription = Fixtures.subscriptionFromJson("model/SubscriptionLocalisation/subscription3/subscription.json")
// val account = Fixtures.accountFromJson("model/SubscriptionLocalisation/subscription3/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("model/SubscriptionLocalisation/subscription3/invoice-preview.json")

class CurrencyAndLocalisationTest extends munit.FunSuite {
  test("determineSubscriptionLocalisation (1): Domestic") {
    val subscription =
      Fixtures.subscriptionFromJson("model/SubscriptionLocalisation/subscription1/subscription.json")
    val account = Fixtures.accountFromJson("model/SubscriptionLocalisation/subscription1/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("model/SubscriptionLocalisation/subscription1/invoice-preview.json")
    val localization = CurrencyAndLocalisation.determineSubscriptionCurrencyAndLocalisation(
      subscription,
      invoicePreview,
      account
    )
    // In this case, we have a USD subscription in the US, so we get Domestic
    assertEquals(localization, Some(CurrencyAndLocalisation("USD", Domestic)))
  }

  test("determineSubscriptionLocalisation (2): ROW (USD Variant, France address)") {
    val subscription =
      Fixtures.subscriptionFromJson("model/SubscriptionLocalisation/subscription2/subscription.json")
    val account = Fixtures.accountFromJson("model/SubscriptionLocalisation/subscription2/account.json")
    val invoicePreview =
      Fixtures.invoiceListFromJson("model/SubscriptionLocalisation/subscription2/invoice-preview.json")
    val localization = CurrencyAndLocalisation.determineSubscriptionCurrencyAndLocalisation(
      subscription,
      invoicePreview,
      account
    )

    assertEquals(subscription.ratePlans.headOption.get.ratePlanCharges.headOption.get.currency, "USD")
    assertEquals(account.soldToContact.country, "France")

    // ROW (USD Variant, France address)
    assertEquals(localization, Some(CurrencyAndLocalisation("USD", RestOfWorld)))
  }
}
