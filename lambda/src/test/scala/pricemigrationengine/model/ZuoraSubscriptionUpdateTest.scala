package pricemigrationengine.model

import java.time.LocalDate
import pricemigrationengine.Fixtures._
import pricemigrationengine.Fixtures

class ZuoraSubscriptionUpdateTest extends munit.FunSuite {

  test("Zuora amendment correctly creates a charge") {
    val fixtureSet = "GuardianWeekly/CappedPriceIncrease2"
    val date = LocalDate.of(2022, 12, 30)

    val update = ZuoraSubscriptionUpdate.zuoraUpdate(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date,
      None
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
              contractEffectiveDate = LocalDate.of(2022, 12, 30),
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
                  billingPeriod = "Quarter",
                  price = 108.0
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(
              ratePlanId = "id",
              contractEffectiveDate = LocalDate.of(2022, 12, 30)
            )
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("Zuora amendment correctly creates a charge override from an enforced priced") {
    val fixtureSet = "GuardianWeekly/CappedPriceIncrease3"
    val date = LocalDate.of(2022, 12, 19)
    val update = ZuoraSubscriptionUpdate.zuoraUpdate(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date,
      Some(37.20)
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0086619bf8901661ab02752722f",
              contractEffectiveDate = LocalDate.of(2022, 12, 19),
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff6619bf8b01661ab2d0396eb2",
                  billingPeriod = "Quarter",
                  price = 37.20
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(
              ratePlanId = "id",
              contractEffectiveDate = LocalDate.of(2022, 12, 19)
            )
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("updateOfRatePlansToCurrent: is correct for a GW ROW subscription with a past Zone ABC rate plan") {
    val fixtureSet = "GuardianWeekly/CappedPriceIncrease4"
    val date = LocalDate.of(2023, 2, 11)
    val update = ZuoraSubscriptionUpdate.zuoraUpdate(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date,
      None
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0086619bf8901661ab02752722f",
              contractEffectiveDate = LocalDate.of(2023, 2, 11),
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff6619bf8b01661ab2d0396eb2",
                  billingPeriod = "Quarter",
                  price = 74.4
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(
              ratePlanId = "id",
              contractEffectiveDate = LocalDate.of(2023, 2, 11)
            )
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test(
    "updateOfRatePlansToCurrent: migrates GW Zone C Quarterly plan to Rest Of World Quarterly plan (billed in USD)"
  ) {
    val fixtureSet = "GuardianWeekly/ZoneABC/ZoneC_USD"
    val date = LocalDate.of(2022, 10, 13)
    val update = ZuoraSubscriptionUpdate.zuoraUpdate(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date,
      None
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0086619bf8901661ab02752722f",
              contractEffectiveDate = date,
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff6619bf8b01661ab2d0396eb2",
                  billingPeriod = "Quarter",
                  price = 90.0
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "gwZoneC", contractEffectiveDate = date)
          ),
          currentTerm = None,
          currentTermPeriodType = None
        )
      )
    )
  }

  test("updateOfRatePlansToCurrent: updates correct rate plans on a standard monthly voucher sub") {
    val fixtureSet = "NewspaperVoucher/Monthly"
    val date = LocalDate.of(2020, 5, 28)
    val update = ZuoraSubscriptionUpdate.zuoraUpdate(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date,
      None
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0fd56fe270b0157040dd79b35da",
              contractEffectiveDate = date,
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff56fe33f0015709c182cb7c82",
                  billingPeriod = "Month",
                  price = 6.7
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fd56fe26b6015709c0613b44a6",
                  billingPeriod = "Month",
                  price = 6.7
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff56fe33f0015709c215527db4",
                  billingPeriod = "Month",
                  price = 6.7
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fd56fe270b015709c320ee0595",
                  billingPeriod = "Month",
                  price = 9.75
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fd56fe26b601570431a5bc5a34",
                  billingPeriod = "Month",
                  price = 6.7
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff56fe33f5015709c39719783e",
                  billingPeriod = "Month",
                  price = 9.74
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff56fe33f3015709c110a71630",
                  billingPeriod = "Month",
                  price = 6.7
                )
              )
            )
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
    val update = ZuoraSubscriptionUpdate.zuoraUpdate(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date,
      None
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0ff56fe33f00157040f9a537f4b",
              contractEffectiveDate = date,
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0ff56fe33f00157041713362b51",
                  billingPeriod = "Month",
                  price = 10.99
                ),
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fc56fe26ba01570417df6d1b54",
                  billingPeriod = "Month",
                  price = 11.0
                )
              )
            )
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
    val update = ZuoraSubscriptionUpdate.zuoraUpdate(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date,
      None
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
    val update = ZuoraSubscriptionUpdate.zuoraUpdate(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date,
      None
    )
    assertEquals(
      update,
      Right(
        ZuoraSubscriptionUpdate(
          add = List(
            AddZuoraRatePlan(
              productRatePlanId = "2c92a0fe6619b4b301661aa494392ee2",
              contractEffectiveDate = date,
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fe6619b4b601661aa8b74e623f",
                  billingPeriod = "Quarter",
                  price = 42.4
                )
              )
            )
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
    val update = ZuoraSubscriptionUpdate.zuoraUpdate(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date,
      None
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
    val update = ZuoraSubscriptionUpdate.zuoraUpdate(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date,
      None
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
    val update = ZuoraSubscriptionUpdate.zuoraUpdate(
      account = accountFromJson(s"$fixtureSet/Account.json"),
      catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json"),
      subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json"),
      invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json"),
      date,
      None
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
              chargeOverrides = List(
                ChargeOverride(
                  productRatePlanChargeId = "2c92a0fe5af9a6b9015b0fe1ed121177",
                  billingPeriod = "Month",
                  price = 11.99
                )
              )
            )
          ),
          remove = List(
            RemoveZuoraRatePlan(ratePlanId = "rp572", contractEffectiveDate = date)
          )
        )
      )
    )
  }

  test("Estimated price is computed correctly") {
    val fixtureSet = "GuardianWeekly/CappedPriceIncrease"
    val catalogue = productCatalogueFromJson(s"$fixtureSet/Catalogue.json")
    val subscription = Fixtures.subscriptionFromJson(s"$fixtureSet/Subscription.json")
    val account = Fixtures.accountFromJson(s"$fixtureSet/Account.json")
    val earliestStartDate = LocalDate.of(2022, 10, 10)
    val invoiceList = Fixtures.invoiceListFromJson(s"$fixtureSet/InvoicePreview.json")
    val cohortSpec = CohortSpec("Cohort1", "Campaign1", LocalDate.of(2023, 4, 1), earliestStartDate)
    val estimationResult =
      EstimationResult(
        account,
        catalogue,
        subscription,
        invoiceList,
        earliestStartDate,
        cohortSpec,
      )
    assertEquals(
      estimationResult,
      Right(EstimationData("subNum", LocalDate.of(2022, 11, 12), "GBP", 30.00, 41.25, "Quarter"))
    )
  }
}
