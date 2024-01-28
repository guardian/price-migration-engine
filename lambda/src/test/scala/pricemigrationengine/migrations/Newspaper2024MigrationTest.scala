package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.migrations.newspaper2024Migration.Estimation._
import pricemigrationengine.migrations.newspaper2024Migration.Amendment._
import pricemigrationengine.migrations.newspaper2024Migration.{Estimation, StaticData}
import pricemigrationengine.migrations.newspaper2024Migration.StaticData.{ChargeDistribution2024, IndividualCharge2024}
import pricemigrationengine.util.StringObfuscation

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
    assertEquals(
      StaticData.priceLookup("Newspaper Delivery", Monthly, "Weekend"),
      Some(BigDecimal(31.99))
    )
    assertEquals(
      StaticData.priceLookup("Newspaper Digital Voucher", Annual, "Sixday"),
      Some(BigDecimal(683.88))
    )
    assertEquals(StaticData.priceLookup("Newspaper Digital Voucher", Annual, "Sixday+"), None)
    assertEquals(StaticData.priceLookup("Non existent product", Annual, "Weekend"), None)
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
    val details =
      SubscriptionData2024(
        "Newspaper Delivery",
        ratePlan,
        "Weekend+",
        Monthly,
        "GBP",
        BigDecimal(33.76),
        "2c92a0ff560d311b0156136b9f5c3968"
      ) // sum of the prices
    assertEquals(subscriptionToSubscriptionData2024(subscription), Right(details))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperHomeDelivery-Quarterly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Quarterly/subscription.json")
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
    val details =
      SubscriptionData2024(
        "Newspaper Delivery",
        ratePlan,
        "Weekend",
        Quarterly,
        "GBP",
        BigDecimal(83.97),
        "2c92a0fd5614305c01561dc88f3275be"
      )
    assertEquals(subscriptionToSubscriptionData2024(subscription), Right(details))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Monthly/subscription.json")
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
    val details =
      SubscriptionData2024(
        "Newspaper Digital Voucher",
        ratePlan,
        "Weekend+",
        Monthly,
        "GBP",
        BigDecimal(31.99),
        "2c92a00870ec598001710740c6672ee7"
      )
    assertEquals(subscriptionToSubscriptionData2024(subscription), Right(details))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Quarterly/subscription.json")
    val ratePlanCharges = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a00870ec598001710740d325302c",
        name = "Sunday",
        number = "C-03619148",
        currency = "GBP",
        price = Some(34.47),
        billingPeriod = Some("Quarter"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 5)),
        processedThroughDate = Some(LocalDate.of(2023, 10, 5)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740d28e3024",
        name = "Saturday",
        number = "C-03619147",
        currency = "GBP",
        price = Some(34.5),
        billingPeriod = Some("Quarter"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 5)),
        processedThroughDate = Some(LocalDate.of(2023, 10, 5)),
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
        "8a128f788af711af018afe4af2112e70",
        "Newspaper Digital Voucher",
        "2c92a00870ec598001710740d24b3022",
        "Weekend",
        ratePlanCharges,
        Some("Add")
      )
    val details =
      SubscriptionData2024(
        "Newspaper Digital Voucher",
        ratePlan,
        "Weekend",
        Quarterly,
        "GBP",
        BigDecimal(68.97),
        "2c92a00870ec598001710740d24b3022"
      )
    assertEquals(subscriptionToSubscriptionData2024(subscription), Right(details))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-SemiAnnual/subscription.json")
    val ratePlanCharges = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a00870ec598001710740cd6e2fa2",
        name = "Saturday",
        number = "C-03922439",
        currency = "GBP",
        price = Some(62.94),
        billingPeriod = Some("Semi_Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 3, 21)),
        processedThroughDate = Some(LocalDate.of(2023, 9, 21)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740cd012f90",
        name = "Wednesday",
        number = "C-03922438",
        currency = "GBP",
        price = Some(45.0),
        billingPeriod = Some("Semi_Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 3, 21)),
        processedThroughDate = Some(LocalDate.of(2023, 9, 21)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740cc9b2f88",
        name = "Thursday",
        number = "C-03922437",
        currency = "GBP",
        price = Some(45.0),
        billingPeriod = Some("Semi_Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 3, 21)),
        processedThroughDate = Some(LocalDate.of(2023, 9, 21)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740cc2c2f80",
        name = "Tuesday",
        number = "C-03922436",
        currency = "GBP",
        price = Some(45.0),
        billingPeriod = Some("Semi_Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 3, 21)),
        processedThroughDate = Some(LocalDate.of(2023, 9, 21)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740cbb32f77",
        name = "Monday",
        number = "C-03922435",
        currency = "GBP",
        price = Some(45.0),
        billingPeriod = Some("Semi_Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 3, 21)),
        processedThroughDate = Some(LocalDate.of(2023, 9, 21)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740cb4e2f6b",
        name = "Friday",
        number = "C-03922434",
        currency = "GBP",
        price = Some(45.0),
        billingPeriod = Some("Semi_Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 3, 21)),
        processedThroughDate = Some(LocalDate.of(2023, 9, 21)),
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
        "8a129cde8ab0847b018ab6520df41c87",
        "Newspaper Digital Voucher",
        "2c92a00870ec598001710740ca532f69",
        "Sixday",
        ratePlanCharges,
        Some("Add")
      )
    val details =
      SubscriptionData2024(
        "Newspaper Digital Voucher",
        ratePlan,
        "Sixday",
        SemiAnnual,
        "GBP",
        BigDecimal(287.94),
        "2c92a00870ec598001710740ca532f69"
      )
    assertEquals(subscriptionToSubscriptionData2024(subscription), Right(details))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperSubscriptionCard-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Annual/subscription.json")
    val ratePlanCharges = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a00870ec598001710740c9d72f61",
        name = "Sunday",
        number = "C-04247184",
        currency = "GBP",
        price = Some(131.28),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 5, 25)),
        processedThroughDate = Some(LocalDate.of(2023, 5, 25)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740c9802f59",
        name = "Wednesday",
        number = "C-04247183",
        currency = "GBP",
        price = Some(93.84),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 5, 25)),
        processedThroughDate = Some(LocalDate.of(2023, 5, 25)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740c91d2f4d",
        name = "Friday",
        number = "C-04247182",
        currency = "GBP",
        price = Some(93.84),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 5, 25)),
        processedThroughDate = Some(LocalDate.of(2023, 5, 25)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740c8c42f40",
        name = "Thursday",
        number = "C-04247181",
        currency = "GBP",
        price = Some(93.84),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 5, 25)),
        processedThroughDate = Some(LocalDate.of(2023, 5, 25)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740c8652f37",
        name = "Saturday",
        number = "C-04247180",
        currency = "GBP",
        price = Some(131.4),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 5, 25)),
        processedThroughDate = Some(LocalDate.of(2023, 5, 25)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740c80f2f26",
        name = "Tuesday",
        number = "C-04247179",
        currency = "GBP",
        price = Some(93.84),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 5, 25)),
        processedThroughDate = Some(LocalDate.of(2023, 5, 25)),
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
        productRatePlanChargeId = "2c92a00870ec598001710740c7b82f1c",
        name = "Monday",
        number = "C-04247178",
        currency = "GBP",
        price = Some(93.84),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 5, 25)),
        processedThroughDate = Some(LocalDate.of(2023, 5, 25)),
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
        "8a128636884d1bc501885172384b76da",
        "Newspaper Digital Voucher",
        "2c92a00870ec598001710740c78d2f13",
        "Everyday",
        ratePlanCharges,
        Some("Add")
      )
    val details =
      SubscriptionData2024(
        "Newspaper Digital Voucher",
        ratePlan,
        "Everyday",
        Annual,
        "GBP",
        BigDecimal(731.88),
        "2c92a00870ec598001710740c78d2f13"
      )
    assertEquals(subscriptionToSubscriptionData2024(subscription), Right(details))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Monthly/subscription.json")
    val ratePlanCharges = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0ff56fe33f5015709b8fc4d5617",
        name = "Sunday",
        number = "C-02302023",
        currency = "GBP",
        price = Some(10.99),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 15)),
        processedThroughDate = Some(LocalDate.of(2023, 12, 15)),
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
        productRatePlanChargeId = "2c92a0fe56fe33ff015709bb986636d8",
        name = "Digipack",
        number = "C-02302022",
        currency = "GBP",
        price = Some(10.0),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 15)),
        processedThroughDate = Some(LocalDate.of(2023, 12, 15)),
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
        productRatePlanChargeId = "2c92a0fd56fe26b601570432f4e33d17",
        name = "Saturday",
        number = "C-02302021",
        currency = "GBP",
        price = Some(11.0),
        billingPeriod = Some("Month"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 15)),
        processedThroughDate = Some(LocalDate.of(2023, 12, 15)),
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
        "8a1288dd877e3a9a018789c707da0528",
        "Newspaper Voucher",
        "2c92a0fd56fe26b60157040cdd323f76",
        "Weekend+",
        ratePlanCharges,
        None
      )
    val details = SubscriptionData2024(
      "Newspaper Voucher",
      ratePlan,
      "Weekend+",
      Monthly,
      "GBP",
      BigDecimal(31.99),
      "2c92a0fd56fe26b60157040cdd323f76"
    )
    assertEquals(subscriptionToSubscriptionData2024(subscription), Right(details))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Quarterly/subscription.json")
    val ratePlanCharges = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0ff56fe33f5015709b8fc4d5617",
        name = "Sunday",
        number = "C-02640789",
        currency = "GBP",
        price = Some(32.97),
        billingPeriod = Some("Quarter"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 26)),
        processedThroughDate = Some(LocalDate.of(2023, 10, 26)),
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
        productRatePlanChargeId = "2c92a0fe56fe33ff015709bb986636d8",
        name = "Digipack",
        number = "C-02640788",
        currency = "GBP",
        price = Some(30.0),
        billingPeriod = Some("Quarter"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 26)),
        processedThroughDate = Some(LocalDate.of(2023, 10, 26)),
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
        productRatePlanChargeId = "2c92a0fd56fe26b601570432f4e33d17",
        name = "Saturday",
        number = "C-02640787",
        currency = "GBP",
        price = Some(33.0),
        billingPeriod = Some("Quarter"),
        chargedThroughDate = Some(LocalDate.of(2024, 1, 26)),
        processedThroughDate = Some(LocalDate.of(2023, 10, 26)),
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
        "8a128e088b64d6f2018b6a75ba4958f8",
        "Newspaper Voucher",
        "2c92a0fd56fe26b60157040cdd323f76",
        "Weekend+",
        ratePlanCharges,
        Some("Add")
      )
    val details =
      SubscriptionData2024(
        "Newspaper Voucher",
        ratePlan,
        "Weekend+",
        Quarterly,
        "GBP",
        BigDecimal(95.97),
        "2c92a0fd56fe26b60157040cdd323f76"
      )
    assertEquals(subscriptionToSubscriptionData2024(subscription), Right(details))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-SemiAnnual/subscription.json")
    val ratePlanCharges = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0ff56fe33f5015709b8fc4d5617",
        name = "Sunday",
        number = "C-02313016",
        currency = "GBP",
        price = Some(65.94),
        billingPeriod = Some("Semi_Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 2, 24)),
        processedThroughDate = Some(LocalDate.of(2023, 8, 24)),
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
        productRatePlanChargeId = "2c92a0fe56fe33ff015709bb986636d8",
        name = "Digipack",
        number = "C-02313015",
        currency = "GBP",
        price = Some(60.0),
        billingPeriod = Some("Semi_Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 2, 24)),
        processedThroughDate = Some(LocalDate.of(2023, 8, 24)),
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
        productRatePlanChargeId = "2c92a0fd56fe26b601570432f4e33d17",
        name = "Saturday",
        number = "C-02313014",
        currency = "GBP",
        price = Some(66.0),
        billingPeriod = Some("Semi_Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 2, 24)),
        processedThroughDate = Some(LocalDate.of(2023, 8, 24)),
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
        "8a128bfc8a217767018a25faf0e44eb3",
        "Newspaper Voucher",
        "2c92a0fd56fe26b60157040cdd323f76",
        "Weekend+",
        ratePlanCharges,
        Some("Add")
      )
    val details =
      SubscriptionData2024(
        "Newspaper Voucher",
        ratePlan,
        "Weekend+",
        SemiAnnual,
        "GBP",
        BigDecimal(191.94),
        "2c92a0fd56fe26b60157040cdd323f76"
      )
    assertEquals(subscriptionToSubscriptionData2024(subscription), Right(details))
  }
  test("Newspaper2024Migration | Rate plan name determination is correct | NewspaperVoucherBook-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Annual/subscription.json")
    val ratePlanCharges = List(
      ZuoraRatePlanCharge(
        productRatePlanChargeId = "2c92a0ff56fe33f5015709b8fc4d5617",
        name = "Sunday",
        number = "C-02808103",
        currency = "GBP",
        price = Some(131.88),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 2, 3)),
        processedThroughDate = Some(LocalDate.of(2023, 2, 3)),
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
        productRatePlanChargeId = "2c92a0fe56fe33ff015709bb986636d8",
        name = "Digipack",
        number = "C-02808102",
        currency = "GBP",
        price = Some(120.0),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 2, 3)),
        processedThroughDate = Some(LocalDate.of(2023, 2, 3)),
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
        productRatePlanChargeId = "2c92a0fd56fe26b601570432f4e33d17",
        name = "Saturday",
        number = "C-02808101",
        currency = "GBP",
        price = Some(132.0),
        billingPeriod = Some("Annual"),
        chargedThroughDate = Some(LocalDate.of(2024, 2, 3)),
        processedThroughDate = Some(LocalDate.of(2023, 2, 3)),
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
        "8a128f76860bed21018615f5f4ba4a40",
        "Newspaper Voucher",
        "2c92a0fd56fe26b60157040cdd323f76",
        "Weekend+",
        ratePlanCharges,
        Some("Add")
      )
    val details = SubscriptionData2024(
      "Newspaper Voucher",
      ratePlan,
      "Weekend+",
      Annual,
      "GBP",
      BigDecimal(383.88),
      "2c92a0fd56fe26b60157040cdd323f76"
    )
    assertEquals(subscriptionToSubscriptionData2024(subscription), Right(details))
  }

  // -- prices -------------------------------------------------------

  // I have added the name of the rate plan name to help manual checking against the original price matrix
  // that the tests are correct.

  test("Newspaper2024Migration | (subscription -> new price) lookup is correct | NewspaperHomeDelivery-Monthly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/subscription.json")
    // rate plan name: Weekend+
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(40.99)))
  }
  test("Newspaper2024Migration | (subscription -> new price) lookup is correct | NewspaperHomeDelivery-Quarterly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Quarterly/subscription.json")
    // rate plan name: Weekend
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(95.97)))
  }
  test("Newspaper2024Migration | (subscription -> new price) lookup is correct | NewspaperSubscriptionCard-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Monthly/subscription.json")
    // rate plan name: Weekend+
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(34.99)))
  }
  test("Newspaper2024Migration | (subscription -> new price) lookup is correct | NewspaperSubscriptionCard-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Quarterly/subscription.json")
    // rate plan name: Weekend
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(77.97)))
  }
  test(
    "Newspaper2024Migration | (subscription -> new price) lookup is correct | NewspaperSubscriptionCard-SemiAnnual"
  ) {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-SemiAnnual/subscription.json")
    // rate plan name: Sixday
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(341.94)))
  }
  test("Newspaper2024Migration | (subscription -> new price) lookup is correct | NewspaperSubscriptionCard-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Annual/subscription.json")
    // rate plan name: Everyday
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(779.88)))
  }
  test("Newspaper2024Migration | (subscription -> new price) lookup is correct | NewspaperVoucherBook-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Monthly/subscription.json")
    // rate plan name: Weekend+
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(34.99)))
  }
  test("Newspaper2024Migration | (subscription -> new price) lookup is correct | NewspaperVoucherBook-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Quarterly/subscription.json")
    // rate plan name: Weekend+
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(104.97)))
  }
  test("Newspaper2024Migration | (subscription -> new price) lookup is correct | NewspaperVoucherBook-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-SemiAnnual/subscription.json")
    // rate plan name: Weekend+
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(209.94)))
  }
  test("Newspaper2024Migration | (subscription -> new price) lookup is correct | NewspaperVoucherBook-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Annual/subscription.json")
    // rate plan name: Weekend+
    assertEquals(subscriptionToNewPrice(subscription), Some(BigDecimal(419.88)))
  }

  // -- price data -------------------------------------------------------

  // I addition of the product and the billing period, which are both obvious from the name of the file
  // to determine the price, we also need the rate pan name, which can be read from the RatePlanDetails.
  // Here is the mapping
  /*
    | Product                                                 | Billing Period | Rate plan name | New price |
    | Newspaper Home Delivery / Newspaper Delivery            | Monthly        | Weekend+       |  40.99    |
    | Newspaper Home Delivery / Newspaper Delivery            | Quarterly      | Weekend        |  95.97    |
    | Newspaper Subscription Card / Newspaper Digital Voucher | Monthly        | Weekend+       |  34.99    |
    | Newspaper Subscription Card / Newspaper Digital Voucher | Quarterly      | Weekend        |  77.97    |
    | Newspaper Subscription Card / Newspaper Digital Voucher | Semi Annual    | Sixday         | 341.94    |
    | Newspaper Subscription Card / Newspaper Digital Voucher | Annual         | Everyday       | 779.88    |
    | Newspaper Voucher Book / Newspaper Voucher              | Monthly        | Weekend+       |  34.99    |
    | Newspaper Voucher Book / Newspaper Voucher              | Quarterly      | Weekend+       | 104.97    |
    | Newspaper Voucher Book / Newspaper Voucher              | Semi Annual    | Weekend+       | 209.94    |
    | Newspaper Voucher Book / Newspaper Voucher              | Annual         | Weekend+       | 419.88    |
   */

  test("Newspaper2024Migration | price data is correct | NewspaperHomeDelivery-Monthly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/subscription.json")
    val priceData = PriceData("GBP", BigDecimal(33.76), BigDecimal(40.99), "Month")
    assertEquals(Estimation.priceData(subscription), Right(priceData))
  }
  test("Newspaper2024Migration | price data is correct | NewspaperHomeDelivery-Quarterly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Quarterly/subscription.json")
    val priceData = PriceData("GBP", BigDecimal(83.97), BigDecimal(95.97), "Quarter")
    assertEquals(Estimation.priceData(subscription), Right(priceData))
  }
  test("Newspaper2024Migration | price data is correct | NewspaperSubscriptionCard-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Monthly/subscription.json")
    val priceData = PriceData("GBP", BigDecimal(31.99), BigDecimal(34.99), "Month")
    assertEquals(Estimation.priceData(subscription), Right(priceData))
  }
  test("Newspaper2024Migration | price data is correct | NewspaperSubscriptionCard-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Quarterly/subscription.json")
    val priceData = PriceData("GBP", BigDecimal(68.97), BigDecimal(77.97), "Quarter")
    assertEquals(Estimation.priceData(subscription), Right(priceData))
  }
  test("Newspaper2024Migration | price data is correct | NewspaperSubscriptionCard-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-SemiAnnual/subscription.json")
    val priceData = PriceData("GBP", BigDecimal(287.94), BigDecimal(341.94), "Semi_Annual")
    assertEquals(Estimation.priceData(subscription), Right(priceData))
  }
  test("Newspaper2024Migration | price data is correct | NewspaperSubscriptionCard-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Annual/subscription.json")
    val priceData = PriceData("GBP", BigDecimal(731.88), BigDecimal(779.88), "Annual")
    assertEquals(Estimation.priceData(subscription), Right(priceData))
  }
  test("Newspaper2024Migration | price data is correct | NewspaperVoucherBook-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Monthly/subscription.json")
    val priceData = PriceData("GBP", BigDecimal(31.99), BigDecimal(34.99), "Month")
    assertEquals(Estimation.priceData(subscription), Right(priceData))
  }
  test("Newspaper2024Migration | price data is correct | NewspaperVoucherBook-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Quarterly/subscription.json")
    val priceData = PriceData("GBP", BigDecimal(95.97), BigDecimal(104.97), "Quarter")
    assertEquals(Estimation.priceData(subscription), Right(priceData))
  }
  test("Newspaper2024Migration | price data is correct | NewspaperVoucherBook-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-SemiAnnual/subscription.json")
    val priceData = PriceData("GBP", BigDecimal(191.94), BigDecimal(209.94), "Semi_Annual")
    assertEquals(Estimation.priceData(subscription), Right(priceData))
  }
  test("Newspaper2024Migration | price data is correct | NewspaperVoucherBook-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Annual/subscription.json")
    val priceData = PriceData("GBP", BigDecimal(383.88), BigDecimal(419.88), "Annual")
    assertEquals(Estimation.priceData(subscription), Right(priceData))
  }

  // -- subscription to batch Id -------------------------------------------------------

  /*
    The important bit here is to check that the monthlies are correctly resolved to the batchId
    NewspaperHomeDelivery-Monthly     -> processedThroughDate: 2023-06-22 -> MonthliesPart1
    NewspaperSubscriptionCard-Monthly -> processedThroughDate: 2021-06-30 -> MonthliesPart1
    NewspaperVoucherBook-Monthly      -> processedThroughDate: 2023-08-15 -> MonthliesPart2
   */

  test("Newspaper2024Migration | batchId is correct | NewspaperHomeDelivery-Monthly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/subscription.json")
    assertEquals(Estimation.subscriptionToBatchId(subscription), Right(MonthliesPart1))
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperHomeDelivery-Quarterly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Quarterly/subscription.json")
    assertEquals(Estimation.subscriptionToBatchId(subscription), Right(MoreThanMonthlies))
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperSubscriptionCard-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Monthly/subscription.json")
    assertEquals(Estimation.subscriptionToBatchId(subscription), Right(MonthliesPart1))
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperSubscriptionCard-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Quarterly/subscription.json")
    assertEquals(Estimation.subscriptionToBatchId(subscription), Right(MoreThanMonthlies))
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperSubscriptionCard-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-SemiAnnual/subscription.json")
    assertEquals(Estimation.subscriptionToBatchId(subscription), Right(MoreThanMonthlies))
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperSubscriptionCard-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Annual/subscription.json")
    assertEquals(Estimation.subscriptionToBatchId(subscription), Right(MoreThanMonthlies))
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperVoucherBook-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Monthly/subscription.json")
    assertEquals(Estimation.subscriptionToBatchId(subscription), Right(MonthliesPart2))
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperVoucherBook-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Quarterly/subscription.json")
    assertEquals(Estimation.subscriptionToBatchId(subscription), Right(MoreThanMonthlies))
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperVoucherBook-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-SemiAnnual/subscription.json")
    assertEquals(Estimation.subscriptionToBatchId(subscription), Right(MoreThanMonthlies))
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperVoucherBook-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Annual/subscription.json")
    assertEquals(Estimation.subscriptionToBatchId(subscription), Right(MoreThanMonthlies))
  }

  // -- spread period -------------------------------------------------------

  /*
    The non monthlies will be resolving to 1, the Part 1 will be 1 and te Part 2 will be 2
    NewspaperHomeDelivery-Monthly     -> processedThroughDate: 2023-06-22 -> MonthliesPart1
    NewspaperSubscriptionCard-Monthly -> processedThroughDate: 2021-06-30 -> MonthliesPart1
    NewspaperVoucherBook-Monthly      -> processedThroughDate: 2023-08-15 -> MonthliesPart2
   */

  test("Newspaper2024Migration | batchId is correct | NewspaperHomeDelivery-Monthly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/subscription.json")
    assertEquals(startDateSpreadPeriod(subscription), 1)
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperHomeDelivery-Quarterly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Quarterly/subscription.json")
    assertEquals(startDateSpreadPeriod(subscription), 1)
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperSubscriptionCard-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Monthly/subscription.json")
    assertEquals(startDateSpreadPeriod(subscription), 1)
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperSubscriptionCard-Quarterly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Quarterly/subscription.json")
    assertEquals(startDateSpreadPeriod(subscription), 1)
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperSubscriptionCard-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-SemiAnnual/subscription.json")
    assertEquals(startDateSpreadPeriod(subscription), 1)
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperSubscriptionCard-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Annual/subscription.json")
    assertEquals(startDateSpreadPeriod(subscription), 1)
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperVoucherBook-Monthly") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Monthly/subscription.json")
    assertEquals(startDateSpreadPeriod(subscription), 2)
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperVoucherBook-Quarterly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Quarterly/subscription.json")
    assertEquals(startDateSpreadPeriod(subscription), 1)
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperVoucherBook-SemiAnnual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-SemiAnnual/subscription.json")
    assertEquals(startDateSpreadPeriod(subscription), 1)
  }
  test("Newspaper2024Migration | batchId is correct | NewspaperVoucherBook-Annual") {
    val subscription =
      Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Annual/subscription.json")
    assertEquals(startDateSpreadPeriod(subscription), 1)
  }

  // -- compatibility between Prices and Price Distributions ----------------------------------------------------------

  // Here we are checking the consistency between prices and price distributions
  // This section perform a check of the form

  assertEquals(
    StaticData.newspaperHomeDeliveryMonthlyPrices.get("Everyday").get,
    StaticData.chargeDistributionToPrice(
      StaticData.newspaperHomeDeliveryMonthlyChargeDistributionsMap.get("Everyday").get
    )
  )

  // For each combination (price mapper, price distribution mapper, rate plan name)
  // Note that the list of available rate plan names is dependent on the frequency itself, therefore both are specified
  // We check both the monthly basic prices, but also the derivative prices computed by the price multiplier

  List(
    (
      StaticData.newspaperHomeDeliveryMonthlyPrices,
      StaticData.newspaperHomeDeliveryMonthlyChargeDistributionsMap,
      List(
        "Everyday",
        "Sixday",
        "Weekend",
        "Saturday",
        "Sunday",
        "Everyday+",
        "Sixday+",
        "Weekend+",
        "Saturday+",
        "Sunday+"
      )
    ),
    (
      StaticData.newspaperHomeDeliveryQuarterlyPrices,
      StaticData.newspaperHomeDeliveryQuarterlyChargeDistributionsMap,
      List(
        "Everyday",
        "Sixday",
        "Weekend",
        "Saturday",
        "Sunday"
      )
    ),
    (
      StaticData.newspaperSubscriptionCardMonthlyPrices,
      StaticData.newspaperSubscriptionCardMonthlyChargeDistributionsMap,
      List(
        "Everyday",
        "Sixday",
        "Weekend",
        "Saturday",
        "Sunday",
        "Everyday+",
        "Sixday+",
        "Weekend+",
        "Saturday+",
        "Sunday+"
      )
    ),
    (
      StaticData.newspaperSubscriptionCardQuarterlyPrices,
      StaticData.newspaperSubscriptionCardQuarterlyChargeDistributionsMap,
      List(
        "Everyday",
        "Sixday",
        "Weekend",
        "Everyday+",
        "Sixday+"
      )
    ),
    (
      StaticData.newspaperSubscriptionCardSemiAnnualPrices,
      StaticData.newspaperSubscriptionCardSemiAnnualChargeDistributionsMap,
      List(
        "Everyday",
        "Sixday",
        "Everyday+"
      )
    ),
    (
      StaticData.newspaperSubscriptionCardAnnualPrices,
      StaticData.newspaperSubscriptionCardAnnualChargeDistributionsMap,
      List(
        "Everyday",
        "Sixday",
        "Weekend"
      )
    ),
    (
      StaticData.newspaperVoucherBookMonthlyPrices,
      StaticData.newspaperVoucherBookMonthlChargeDistibutionsMap,
      List(
        "Everyday",
        "Sixday",
        "Weekend",
        "Saturday",
        "Sunday",
        "Everyday+",
        "Sixday+",
        "Weekend+",
        "Saturday+",
        "Sunday+"
      )
    ),
    (
      StaticData.newspaperVoucherBookQuarterlyPrices,
      StaticData.newspaperVoucherBookQuarterlyChargeDistibutionsMap,
      List(
        "Everyday",
        "Sixday",
        "Weekend",
        "Everyday+",
        "Sixday+",
        "Weekend+",
        "Sunday+"
      )
    ),
    (
      StaticData.newspaperVoucherBookSemiAnnualPrices,
      StaticData.newspaperVoucherBookSemiAnnualChargeDistributionsMap,
      List(
        "Everyday",
        "Sixday",
        "Weekend",
        "Everyday+",
        "Sixday+",
        "Weekend+",
        "Sunday+"
      )
    ),
    (
      StaticData.newspaperVoucherBookAnnualPrices,
      StaticData.newspaperVoucherBookAnnualChargeDistributionsMap,
      List(
        "Everyday",
        "Sixday",
        "Weekend",
        "Everyday+",
        "Sixday+",
        "Weekend+",
      )
    )
  ).foreach {
    case (
          mapper1: Map[String, BigDecimal],
          mapper2: Map[String, StaticData.ChargeDistribution2024],
          ratePlanNames: List[String]
        ) =>
      ratePlanNames.foreach { ratePlanName =>
        assertEquals(
          mapper1(ratePlanName),
          StaticData.chargeDistributionToPrice(mapper2(ratePlanName))
        )
      }
  }

  // -- subscriptionToNewChargeDistribution2024 ------------------------------------------------------------------------

  /*
  | Product                                                 | Billing Period | Rate plan name | New price |
  | Newspaper Home Delivery / Newspaper Delivery            | Monthly        | Weekend+       |  40.99    |
  | Newspaper Home Delivery / Newspaper Delivery            | Quarterly      | Weekend        |  95.97    |
  | Newspaper Subscription Card / Newspaper Digital Voucher | Monthly        | Weekend+       |  34.99    |
  | Newspaper Subscription Card / Newspaper Digital Voucher | Quarterly      | Weekend        |  77.97    |
  | Newspaper Subscription Card / Newspaper Digital Voucher | Semi Annual    | Sixday         | 341.94    |
  | Newspaper Subscription Card / Newspaper Digital Voucher | Annual         | Everyday       | 779.88    |
  | Newspaper Voucher Book / Newspaper Voucher              | Monthly        | Weekend+       |  34.99    |
  | Newspaper Voucher Book / Newspaper Voucher              | Quarterly      | Weekend+       | 104.97    |
  | Newspaper Voucher Book / Newspaper Voucher              | Semi Annual    | Weekend+       | 209.94    |
  | Newspaper Voucher Book / Newspaper Voucher              | Annual         | Weekend+       | 419.88    |
   */

  test("Newspaper2024Migration | subscriptionToNewChargeDistribution2024 | NewspaperHomeDelivery-Monthly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/subscription.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/invoice-preview.json")
    // val account = Fixtures.accountFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/account.json")
    // val catalogue = Fixtures.productCatalogueFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/catalogue.json")
    assertEquals(
      subscriptionToNewChargeDistribution2024(subscription),
      Some(
        ChargeDistribution2024(
          monday = None,
          tuesday = None,
          wednesday = None,
          thursday = None,
          friday = None,
          saturday = Some(IndividualCharge2024("2c92a0ff560d311b0156136ba11539ae", BigDecimal(15.99))),
          sunday = Some(IndividualCharge2024("2c92a0ff560d311b0156136ba0523996", BigDecimal(16.0))),
          digitalPack = Some(IndividualCharge2024("2c92a0ff560d311b0156136b9fac3976", BigDecimal(9.0))),
        )
      )
    )
  }

  test("Newspaper2024Migration | subscriptionToNewChargeDistribution2024 | NewspaperSubscriptionCard-Annual") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Annual/subscription.json")
    assertEquals(
      subscriptionToNewChargeDistribution2024(subscription),
      Some(
        ChargeDistribution2024(
          monday = Some(IndividualCharge2024("2c92a00870ec598001710740c7b82f1c", BigDecimal(101.04))),
          tuesday = Some(IndividualCharge2024("2c92a00870ec598001710740c80f2f26", BigDecimal(101.04))),
          wednesday = Some(IndividualCharge2024("2c92a00870ec598001710740c9802f59", BigDecimal(101.04))),
          thursday = Some(IndividualCharge2024("2c92a00870ec598001710740c8c42f40", BigDecimal(101.04))),
          friday = Some(IndividualCharge2024("2c92a00870ec598001710740c91d2f4d", BigDecimal(101.04))),
          saturday = Some(IndividualCharge2024("2c92a00870ec598001710740c8652f37", BigDecimal(137.28))),
          sunday = Some(IndividualCharge2024("2c92a00870ec598001710740c9d72f61", BigDecimal(137.40))),
          digitalPack = None
        )
      )
    )
  }

  test("Newspaper2024Migration | subscriptionToNewChargeDistribution2024 | NewspaperVoucherBook-Annual") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Annual/subscription.json")
    assertEquals(
      subscriptionToNewChargeDistribution2024(subscription),
      Some(
        ChargeDistribution2024(
          None,
          None,
          None,
          None,
          None,
          saturday = Some(IndividualCharge2024("2c92a0fd56fe26b601570432f4e33d17", BigDecimal(155.88))),
          sunday = Some(IndividualCharge2024("2c92a0ff56fe33f5015709b8fc4d5617", BigDecimal(156.0))),
          digitalPack = Some(IndividualCharge2024("2c92a0fe56fe33ff015709bb986636d8", BigDecimal(108.0)))
        )
      )
    )
  }

  // -- subscriptionToZuoraSubscriptionUpdate ------------------------------------------------------------------------

  test("Newspaper2024Migration | subscriptionToZuoraSubscriptionUpdate | NewspaperHomeDelivery-Monthly") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/subscription.json")
    // val invoicePreview = Fixtures.invoiceListFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/invoice-preview.json")
    // val account = Fixtures.accountFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/account.json")
    // val catalogue = Fixtures.productCatalogueFromJson("Newspaper2024/NewspaperHomeDelivery-Monthly/catalogue.json")
    assertEquals(
      subscriptionToZuoraSubscriptionUpdate(subscription, LocalDate.of(2024, 3, 1)),
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0ff560d311b0156136b9f5c3968",
              contractEffectiveDate = LocalDate.of(2024, 3, 1),
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff560d311b0156136ba11539ae",
                  billingPeriod = "Month",
                  price = BigDecimal(15.99)
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff560d311b0156136ba0523996",
                  billingPeriod = "Month",
                  price = BigDecimal(16.0)
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff560d311b0156136b9fac3976",
                  billingPeriod = "Month",
                  price = BigDecimal(9.0)
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(
              ratePlanId = "8a1280478c3a4e1d018c41aa77651cde",
              LocalDate.of(2024, 3, 1)
            )
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("Newspaper2024Migration | subscriptionToZuoraSubscriptionUpdate | NewspaperSubscriptionCard-Annual") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperSubscriptionCard-Annual/subscription.json")
    assertEquals(
      subscriptionToZuoraSubscriptionUpdate(subscription, LocalDate.of(2024, 3, 1)),
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a00870ec598001710740c78d2f13",
              contractEffectiveDate = LocalDate.of(2024, 3, 1),
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a00870ec598001710740c7b82f1c",
                  billingPeriod = "Annual",
                  price = BigDecimal(101.04)
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a00870ec598001710740c80f2f26",
                  billingPeriod = "Annual",
                  price = BigDecimal(101.04)
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a00870ec598001710740c9802f59",
                  billingPeriod = "Annual",
                  price = BigDecimal(101.04)
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a00870ec598001710740c8c42f40",
                  billingPeriod = "Annual",
                  price = BigDecimal(101.04)
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a00870ec598001710740c91d2f4d",
                  billingPeriod = "Annual",
                  price = BigDecimal(101.04)
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a00870ec598001710740c8652f37",
                  billingPeriod = "Annual",
                  price = BigDecimal(137.28)
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a00870ec598001710740c9d72f61",
                  billingPeriod = "Annual",
                  price = BigDecimal(137.40)
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(
              ratePlanId = "8a128636884d1bc501885172384b76da",
              LocalDate.of(2024, 3, 1)
            )
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("Newspaper2024Migration | subscriptionToZuoraSubscriptionUpdate | NewspaperVoucherBook-Annual") {
    val subscription = Fixtures.subscriptionFromJson("Newspaper2024/NewspaperVoucherBook-Annual/subscription.json")
    assertEquals(
      subscriptionToZuoraSubscriptionUpdate(subscription, LocalDate.of(2024, 3, 1)),
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0fd56fe26b60157040cdd323f76",
              contractEffectiveDate = LocalDate.of(2024, 3, 1),
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fd56fe26b601570432f4e33d17",
                  billingPeriod = "Annual",
                  price = BigDecimal(155.88)
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff56fe33f5015709b8fc4d5617",
                  billingPeriod = "Annual",
                  price = BigDecimal(156.0)
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fe56fe33ff015709bb986636d8",
                  billingPeriod = "Annual",
                  price = BigDecimal(108.0)
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(
              ratePlanId = "8a128f76860bed21018615f5f4ba4a40",
              LocalDate.of(2024, 3, 1)
            )
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("String Obfuscation") {
    assertEquals(StringObfuscation.obfuscate("A-Testing0"), "1&3esting4")
  }

}
