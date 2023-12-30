package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.handlers.NotificationHandler.thereIsEnoughNotificationLeadTime
import pricemigrationengine.migrations.Newspaper2024Migration._
import pricemigrationengine.model.CohortTableFilter.SalesforcePriceRiceCreationComplete

/*
  Correspondence between product names in Salesforce versus Zuora

  -----------------------------------------------------------------
  Product name in the Salesforce export | Product name in Zuora
  -----------------------------------------------------------------
  Newspaper - Home Delivery             | Newspaper Delivery
  Newspaper - Subscription Card         | Newspaper Digital Voucher
  Newspaper - Voucher Book              | Newspaper Voucher
  -----------------------------------------------------------------
 */

class Newspaper2024MigrationTest extends munit.FunSuite {

  test("Newspaper2024Migration: Price lookup is correct") {
    assertEquals(priceLookup("Newspaper Delivery", Monthly, "Weekend"), Some(BigDecimal(31.99)))
    assertEquals(priceLookup("Newspaper Digital Voucher", Annual, "Sixday"), Some(BigDecimal(683.88)))
    assertEquals(priceLookup("Newspaper Digital Voucher", Annual, "Sixday+"), None)
    assertEquals(priceLookup("Non existent product", Annual, "Weekend"), None)
  }

  // -- product name -------------------------------------------------------

  test("Newspaper2024Migration | migration product name is correct | NewspaperHomeDelivery-Monthly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/subscription.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/invoice-preview.json")
    // val account = Fixtures.accountFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/account.json")
    // val catalogue = Fixtures.productCatalogueFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/catalogue.json")
    assertEquals(subscriptionToMigrationProductName(subscription), Right("Newspaper Delivery"))
  }
  test("Newspaper2024Migration | migration product name is correct | NewspaperHomeDelivery-Quarterly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Quarterly/subscription.json")
    assertEquals(subscriptionToMigrationProductName(subscription), Right("Newspaper Delivery"))
  }

  test("Newspaper2024Migration | migration product name is correct | NewspaperSubscriptionCard-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Monthly/subscription.json")
    assertEquals(subscriptionToMigrationProductName(subscription), Right("Newspaper Digital Voucher"))
  }
  test("Newspaper2024Migration | migration product name is correct | NewspaperSubscriptionCard-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Quarterly/subscription.json")
    assertEquals(subscriptionToMigrationProductName(subscription), Right("Newspaper Digital Voucher"))
  }
  test("Newspaper2024Migration | migration product name is correct | NewspaperSubscriptionCard-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-SemiAnnual/subscription.json")
    assertEquals(subscriptionToMigrationProductName(subscription), Right("Newspaper Digital Voucher"))
  }
  test("Newspaper2024Migration | migration product name is correct | NewspaperSubscriptionCard-Annual") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Annual/subscription.json")
    assertEquals(subscriptionToMigrationProductName(subscription), Right("Newspaper Digital Voucher"))
  }

  test("Newspaper2024Migration | migration product name is correct | NewspaperVoucherBook-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Monthly/subscription.json")
    assertEquals(subscriptionToMigrationProductName(subscription), Right("Newspaper Voucher"))
  }
  test("Newspaper2024Migration | migration product name is correct | NewspaperVoucherBook-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Quarterly/subscription.json")
    assertEquals(subscriptionToMigrationProductName(subscription), Right("Newspaper Voucher"))
  }
  test("Newspaper2024Migration | migration product name is correct | NewspaperVoucherBook-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-SemiAnnual/subscription.json")
    assertEquals(subscriptionToMigrationProductName(subscription), Right("Newspaper Voucher"))
  }
  test("Newspaper2024Migration | migration product name is correct | NewspaperVoucherBook-Annual") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Annual/subscription.json")
    assertEquals(subscriptionToMigrationProductName(subscription), Right("Newspaper Voucher"))
  }

  // -- rate plan details -------------------------------------------------------

  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperHomeDelivery-Monthly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/subscription.json")
    val productName = subscriptionToMigrationProductName(subscription).toOption.get
    val ratePlanCharges = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0ff560d311b0156136ba11539ae",
        name = "Saturday",
        number = "C-00347069",
        currency = "GBP",
        price = Some(12.37),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2023, 12, 21)),
        processedThroughDate = Some(LocalDate.of(2023, 11, 21)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      ),
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0ff560d311b0156136ba0523996",
        name = "Sunday",
        number = "C-00347067",
        currency = "GBP",
        price = Some(12.72),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2023, 12, 21)),
        processedThroughDate = Some(LocalDate.of(2023, 11, 21)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      ),
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0ff560d311b0156136b9fac3976",
        name = "Digital Pack",
        number = "C-00347064",
        currency = "GBP",
        price = Some(8.67),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2023, 12, 21)),
        processedThroughDate = Some(LocalDate.of(2023, 11, 21)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      )
    )
    val ratePlan =
      ZuoraRatePlan(
        "8a1280478c3a4e1d018c41aa77651cde",
        "Newspaper Delivery",
        "2c92a0ff560d311b0156136b9f5c3968",
        "Weekend+",
        ratePlanCharges,
        None
      )
    val details = RatePlanDetails(ratePlan, "Weekend+", Monthly, "GBP", BigDecimal(33.76)) // sum of the prices
    assertEquals(subscriptionToRatePlanDetails(subscription, productName), Right(details))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperHomeDelivery-Quarterly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Quarterly/subscription.json")
    val productName = subscriptionToMigrationProductName(subscription).toOption.get
    val ratePlanCharges = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0fd5614305c01561dc88fb875d0",
        name = "Saturday",
        number = "C-03643264",
        currency = "GBP",
        price = Some(42.0),
        billingPeriod = Some("Quarter"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 9)),
        processedThroughDate = Some(LocalDate.of(2023, 10, 9)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      ),
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0fd5614305c01561dc88f8975c8",
        name = "Sunday",
        number = "C-03643263",
        currency = "GBP",
        price = Some(41.97),
        billingPeriod = Some("Quarter"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 9)),
        processedThroughDate = Some(LocalDate.of(2023, 10, 9)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      )
    )
    val ratePlan =
      ZuoraRatePlan(
        "8a12837e8b43393e018b4597ea793a61",
        "Newspaper Delivery",
        "2c92a0fd5614305c01561dc88f3275be",
        "Weekend",
        ratePlanCharges,
        Some("Add")
      )
    val details = RatePlanDetails(ratePlan, "Weekend", Quarterly, "GBP", BigDecimal(83.97))
    assertEquals(subscriptionToRatePlanDetails(subscription, productName), Right(details))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Monthly/subscription.json")
    val productName = subscriptionToMigrationProductName(subscription).toOption.get
    val ratePlanCharges = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a00870ec598001710740c7132efe",
        name = "Sunday",
        number = "C-03031284",
        currency = "GBP",
        price = Some(10.99),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2023, 12, 30)),
        processedThroughDate = Some(LocalDate.of(2023, 11, 30)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      ),
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a00870ec598001710740c6ce2ef1",
        name = "Digipack",
        number = "C-03031283",
        currency = "GBP",
        price = Some(10.0),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2023, 12, 30)),
        processedThroughDate = Some(LocalDate.of(2023, 11, 30)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      ),
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a00870ec598001710740c6872ee9",
        name = "Saturday",
        number = "C-03031282",
        currency = "GBP",
        price = Some(11.0),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2023, 12, 30)),
        processedThroughDate = Some(LocalDate.of(2023, 11, 30)),
        specificBillingPeriod = None,
        endDateCondition = Some("Subscription_End"),
        upToPeriodsType = None,
        upToPeriods = None,
        billingDay = Some("ChargeTriggerDay"),
        triggerEvent = Some("CustomerAcceptance"),
        triggerDate = None,
        discountPercentage = None
      )
    )
    val ratePlan =
      ZuoraRatePlan(
        "8a129deb8c3a7a68018c47c946c75e58",
        "Newspaper Digital Voucher",
        "2c92a00870ec598001710740c6672ee7",
        "Weekend+",
        ratePlanCharges,
        Some("Add")
      )
    val details = RatePlanDetails(ratePlan, "Weekend+", Monthly, "GBP", BigDecimal(31.99))
    assertEquals(subscriptionToRatePlanDetails(subscription, productName), Right(details))
  }
  /*
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Quarterly/subscription.json")
    val productName = subscriptionToMigrationProductName(subscription).toOption.get
    assertEquals(subscriptionToRatePlanDetails(subscription, productName), Right("Weekend", Quarterly))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-SemiAnnual/subscription.json")
    val productName = subscriptionToMigrationProductName(subscription).toOption.get
    assertEquals(subscriptionToRatePlanDetails(subscription, productName), Right("Sixday", SemiAnnual))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Annual/subscription.json")
    val productName = subscriptionToMigrationProductName(subscription).toOption.get
    assertEquals(subscriptionToRatePlanDetails(subscription, productName), Right("Everyday", Annual))
  }

  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Monthly/subscription.json")
    val productName = subscriptionToMigrationProductName(subscription).toOption.get
    assertEquals(subscriptionToRatePlanDetails(subscription, productName), Right("Weekend+", Monthly))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Quarterly/subscription.json")
    val productName = subscriptionToMigrationProductName(subscription).toOption.get
    assertEquals(subscriptionToRatePlanDetails(subscription, productName), Right("Weekend+", Quarterly))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-SemiAnnual/subscription.json")
    val productName = subscriptionToMigrationProductName(subscription).toOption.get
    assertEquals(subscriptionToRatePlanDetails(subscription, productName), Right("Weekend+", SemiAnnual))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Annual/subscription.json")
    val productName = subscriptionToMigrationProductName(subscription).toOption.get
    assertEquals(subscriptionToRatePlanDetails(subscription, productName), Right("Weekend+", Annual))
  }
   */

  // -- prices -------------------------------------------------------

  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperHomeDelivery-Monthly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/subscription.json")
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(40.99)))
  }
  /*
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperHomeDelivery-Quarterly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Quarterly/subscription.json")
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(95.97)))
  }

  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Monthly/subscription.json")
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(34.99)))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Quarterly/subscription.json")
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(77.97)))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-SemiAnnual/subscription.json")
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(341.94)))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Annual/subscription.json")
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(779.88)))
  }

  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Monthly/subscription.json")
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(34.99)))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Quarterly/subscription.json")
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(104.97)))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-SemiAnnual/subscription.json")
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(209.94)))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Annual/subscription.json")
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(419.88)))
  }
   */
  // -- price data -------------------------------------------------------

  test("Newspaper2024Migration | price data is correct | NewspaperHomeDelivery-Monthly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/subscription.json")
    val priceData = PriceData("GBP", BigDecimal(33.76), BigDecimal(40.99), "Month")
    assertEquals(Newspaper2024Migration.priceData(subscription), Right(priceData))
  }
  /*
test("Newspaper2024Migration | price data is correct | NewspaperHomeDelivery-Quarterly") {
  val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Quarterly/subscription.json")
  val priceData = PriceData("GBP", BigDecimal(40.99), BigDecimal(40.99), "Monthly")
  assertEquals(Newspaper2024Migration.priceData(subscription), Right(priceData))
}

test("Newspaper2024Migration | price data is correct | NewspaperSubscriptionCard-Monthly") {
  val subscription =
    Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Monthly/subscription.json")
  val priceData = PriceData("GBP", BigDecimal(40.99), BigDecimal(40.99), "Monthly")
  assertEquals(Newspaper2024Migration.priceData(subscription), Right(priceData))
}
test("Newspaper2024Migration | price data is correct | NewspaperSubscriptionCard-Quarterly") {
  val subscription =
    Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Quarterly/subscription.json")
  val priceData = PriceData("GBP", BigDecimal(40.99), BigDecimal(40.99), "Monthly")
  assertEquals(Newspaper2024Migration.priceData(subscription), Right(priceData))
}
test("Newspaper2024Migration | price data is correct | NewspaperSubscriptionCard-SemiAnnual") {
  val subscription =
    Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-SemiAnnual/subscription.json")
  val priceData = PriceData("GBP", BigDecimal(40.99), BigDecimal(40.99), "Monthly")
  assertEquals(Newspaper2024Migration.priceData(subscription), Right(priceData))
}
test("Newspaper2024Migration | price data is correct | NewspaperSubscriptionCard-Annual") {
  val subscription =
    Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Annual/subscription.json")
  val priceData = PriceData("GBP", BigDecimal(40.99), BigDecimal(40.99), "Monthly")
  assertEquals(Newspaper2024Migration.priceData(subscription), Right(priceData))
}

test("Newspaper2024Migration | price data is correct | NewspaperVoucherBook-Monthly") {
  val subscription =
    Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Monthly/subscription.json")
  val priceData = PriceData("GBP", BigDecimal(40.99), BigDecimal(40.99), "Monthly")
  assertEquals(Newspaper2024Migration.priceData(subscription), Right(priceData))
}
test("Newspaper2024Migration | price data is correct | NewspaperVoucherBook-Quarterly") {
  val subscription =
    Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Quarterly/subscription.json")
  val priceData = PriceData("GBP", BigDecimal(40.99), BigDecimal(40.99), "Monthly")
  assertEquals(Newspaper2024Migration.priceData(subscription), Right(priceData))
}
test("Newspaper2024Migration | price data is correct | NewspaperVoucherBook-SemiAnnual") {
  val subscription =
    Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-SemiAnnual/subscription.json")
  val priceData = PriceData("GBP", BigDecimal(40.99), BigDecimal(40.99), "Monthly")
  assertEquals(Newspaper2024Migration.priceData(subscription), Right(priceData))
}
test("Newspaper2024Migration | price data is correct | NewspaperVoucherBook-Annual") {
  val subscription =
    Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Annual/subscription.json")
  val priceData = PriceData("GBP", BigDecimal(40.99), BigDecimal(40.99), "Monthly")
  assertEquals(Newspaper2024Migration.priceData(subscription), Right(priceData))
}
   */
}
