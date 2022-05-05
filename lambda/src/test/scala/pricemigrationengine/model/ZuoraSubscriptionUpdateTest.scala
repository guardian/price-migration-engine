package pricemigrationengine.model

import java.time.LocalDate

import pricemigrationengine.Fixtures
import pricemigrationengine.Fixtures.productCatalogueFromJson
import pricemigrationengine.model.ZuoraProductCatalogue.productPricingMap

class ZuoraSubscriptionUpdateTest extends munit.FunSuite {

  test("updateOfRatePlansToCurrent: updates correct rate plans on a standard monthly voucher sub") {
    val fixtureSet = "NewspaperVoucher/Monthly"
    val date = LocalDate.of(2020, 5, 28)
    val update = ZuoraSubscriptionUpdate.updateOfRatePlansToCurrent(
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(productRatePlanId = "2c92a0fd56fe270b0157040dd79b35da", contractEffectiveDate = date)
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "rp2", contractEffectiveDate = date)
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("updateOfRatePlansToCurrent: updates correct rate plans on a discounted monthly voucher sub") {
    val fixtureSet = "NewspaperVoucher/MonthlyDiscounted"
    val date = LocalDate.of(2020, 6, 15)
    val update = ZuoraSubscriptionUpdate.updateOfRatePlansToCurrent(
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(productRatePlanId = "2c92a0ff56fe33f00157040f9a537f4b", contractEffectiveDate = date)
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "rp2", contractEffectiveDate = date)
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("updateOfRatePlansToCurrent: updates correct rate plans on a quarterly voucher sub") {
    val fixtureSet = "NewspaperVoucher/QuarterlyVoucher"
    val date = LocalDate.of(2020, 7, 5)
    val update = ZuoraSubscriptionUpdate.updateOfRatePlansToCurrent(
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0ff56fe33f00157040f9a537f4b",
              contractEffectiveDate = date,
              chargeOverrides = Seq(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff56fe33f00157041713362b51",
                  billingPeriod = "Quarter",
                  price = 32.97
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fc56fe26ba01570417df6d1b54",
                  billingPeriod = "Quarter",
                  price = 33.00
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "rp456", contractEffectiveDate = date)
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("updateOfRatePlansToCurrent: updates correct rate plans on a quarterly GW sub") {
    val fixtureSet = "QuarterlyGW"
    val date = LocalDate.of(2020, 7, 28)
    val update = ZuoraSubscriptionUpdate.updateOfRatePlansToCurrent(
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2", contractEffectiveDate = date)
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "rp123", contractEffectiveDate = date)
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("updateOfRatePlansToCurrent: updates correct rate plans on a semi-annual voucher sub") {
    val fixtureSet = "NewspaperVoucher/SemiAnnualVoucher"
    val date = LocalDate.of(2020, 7, 13)
    val update = ZuoraSubscriptionUpdate.updateOfRatePlansToCurrent(
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0fd56fe270b0157040e42e536ef",
              contractEffectiveDate = date,
              chargeOverrides = Seq(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fd56fe26b601570431210a310e",
                  billingPeriod = "Semi_Annual",
                  price = 42.00
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fd56fe270b015709be701e78b6",
                  billingPeriod = "Semi_Annual",
                  price = 42.00
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff56fe33f5015709bf7fdd6a4d",
                  billingPeriod = "Semi_Annual",
                  price = 59.94
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fd56fe270b015709bd2d3d75d7",
                  billingPeriod = "Semi_Annual",
                  price = 42.00
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fc56fe26ba015709bee15d653a",
                  billingPeriod = "Semi_Annual",
                  price = 42.00
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fe56fe33ff015709bdb6153cd4",
                  billingPeriod = "Semi_Annual",
                  price = 42.00
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "rp42", contractEffectiveDate = date)
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("updateOfRatePlansToCurrent: updates correct rate plans on an annual voucher sub") {
    val fixtureSet = "NewspaperVoucher/AnnualVoucher"
    val date = LocalDate.of(2020, 12, 7)
    val update = ZuoraSubscriptionUpdate.updateOfRatePlansToCurrent(
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0ff56fe33f00157040f9a537f4b",
              contractEffectiveDate = date,
              chargeOverrides = Seq(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff56fe33f00157041713362b51",
                  billingPeriod = "Annual",
                  price = 131.88
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fc56fe26ba01570417df6d1b54",
                  billingPeriod = "Annual",
                  price = 132.00
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "rp571", contractEffectiveDate = date)
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("updateOfRatePlansToCurrent: extends term when term ends before effective date of update") {
    val fixtureSet = "NewspaperVoucher/TermEndsEarly"
    val date = LocalDate.of(2020, 8, 5)
    val update = ZuoraSubscriptionUpdate.updateOfRatePlansToCurrent(
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          currentTerm = Some(393),
          currentTermPeriodType = Some("Day"),
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0fe5af9a6b9015b0fe1ecc0116c",
              contractEffectiveDate = date,
              chargeOverrides = Nil
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "rp572", contractEffectiveDate = date)
          )
        )
      )
    )
  }

  test("updateOfRatePlansToCurrent: updates correct rate plans on an echo-legacy sub (weekend)") {
    val fixtureSet = "NewspaperDelivery/EchoLegacy/WeekendMonthly"
    val date = LocalDate.of(2022, 6, 7)
    val update = ZuoraSubscriptionUpdate.updateOfRatePlansToCurrent(
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0fd5614305c01561dc88f3275be",
              contractEffectiveDate = date,
              chargeOverrides = Nil
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "echo-legacy", contractEffectiveDate = date)
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("updateOfRatePlansToCurrent: updates correct rate plans on an echo-legacy sub (everyday)") {
    val fixtureSet = "NewspaperDelivery/EchoLegacy/EverydayMonthly"
    val date = LocalDate.of(2022, 9, 28)
    val update = ZuoraSubscriptionUpdate.updateOfRatePlansToCurrent(
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0fd560d13880156136b72e50f0c",
              contractEffectiveDate = date,
              chargeOverrides = Nil
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "echo-legacy", contractEffectiveDate = date)
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("updateOfRatePlansToCurrent: updates correct rate plans on an echo-legacy sub (Saturday Quarterly)") {
    val fixtureSet = "NewspaperDelivery/EchoLegacy/SaturdayQuarterly"
    val date = LocalDate.of(2022, 11, 30)
    val update = ZuoraSubscriptionUpdate.updateOfRatePlansToCurrent(
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0fd5e1dcf0d015e3cb39d0a7ddb",
              contractEffectiveDate = date,
              chargeOverrides = Seq(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fd5e1dcf0d015e3cb39d207ddf",
                  billingPeriod = "Quarter",
                  price = 53.97
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "echo-legacy", contractEffectiveDate = date)
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }
}
