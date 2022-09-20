package pricemigrationengine.handlers

import pricemigrationengine.Fixtures.{invoiceListFromJson, subscriptionFromJson}
import pricemigrationengine.handlers.EstimationHandler.spreadEarliestStartDate
import pricemigrationengine.model.CohortTableFilter.{
  CappedPriceIncrease,
  EstimationComplete,
  NoPriceIncrease,
  ReadyForEstimation
}
import pricemigrationengine.model._
import pricemigrationengine.service.{MockCohortTable, MockZuora}
import zio._
import zio.mock.Expectation._
import zio.test.Assertion._
import zio.test._

import java.time._

/** Mocked specs of EstimationHandler.
  *
  * These specs can be quite difficult to debug when they fail or crash.
  * If a test crashes with a NPE, it usually means there's a mock value missing or it has an unexpected value.
  * The easiest way to find out why a spec is failing is to add a debug statement to
  * <code>EstimationHandler.doEstimation</code>.
  * eg.
  * <p><code>doEstimation(catalogue, item, earliestStartDate).debug("*** estimation")</code></p>
  * Hopefully the testing infrastructure will improve over time.
  */
object EstimationHandlerSpec extends ZIOSpecDefault {

  private val time = OffsetDateTime.of(LocalDateTime.of(2022, 5, 16, 10, 2), ZoneOffset.ofHours(0)).toInstant

  private val subscription = ZuoraSubscription(
    subscriptionNumber = "S1",
    accountNumber = "A9107",
    customerAcceptanceDate = LocalDate.of(2022, 1, 1),
    contractEffectiveDate = LocalDate.of(2022, 1, 1),
    ratePlans = List(
      ZuoraRatePlan(
        id = "R1",
        productName = "P1",
        productRatePlanId = "PRP1",
        ratePlanName = "RP1",
        ratePlanCharges = List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "PRPC1",
            name = "N2",
            number = "C3",
            currency = "GBP",
            price = Some(BigDecimal("1.23")),
            billingPeriod = Some("Month")
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "PRPC4",
            name = "N2",
            number = "C4",
            currency = "GBP",
            price = Some(BigDecimal("1.24")),
            billingPeriod = Some("Month")
          )
        )
      )
    ),
    accountId = "A11",
    status = "Active",
    termStartDate = LocalDate.of(2022, 1, 1),
    termEndDate = LocalDate.of(2023, 1, 1)
  )

  private val subscription2 = ZuoraSubscription(
    subscriptionNumber = "S1",
    accountNumber = "A9107",
    customerAcceptanceDate = LocalDate.of(2022, 1, 1),
    contractEffectiveDate = LocalDate.of(2022, 1, 1),
    ratePlans = List(
      ZuoraRatePlan(
        id = "R1",
        productName = "P2",
        productRatePlanId = "PRP1",
        ratePlanName = "RP1",
        ratePlanCharges = List(
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "PRPC1",
            name = "N2",
            number = "C3",
            currency = "GBP",
            price = Some(BigDecimal("1.23")),
            billingPeriod = Some("Month")
          ),
          ZuoraRatePlanCharge(
            productRatePlanChargeId = "PRPC4",
            name = "N4",
            number = "C4",
            currency = "GBP",
            price = Some(BigDecimal("1.24")),
            billingPeriod = Some("Month")
          )
        )
      )
    ),
    accountId = "A11",
    status = "Active",
    termStartDate = LocalDate.of(2022, 1, 1),
    termEndDate = LocalDate.of(2023, 1, 1)
  )

  private val account = ZuoraAccount(
    soldToContact = SoldToContact(
      country = "United States"
    )
  )

  private val invoicePreview = ZuoraInvoiceList(
    Seq(
      ZuoraInvoiceItem(
        subscriptionNumber = "S1",
        serviceStartDate = LocalDate.of(2022, 5, 1),
        chargeNumber = "C1",
        productName = "P1"
      ),
      ZuoraInvoiceItem(
        subscriptionNumber = "S1",
        serviceStartDate = LocalDate.of(2022, 6, 1),
        chargeNumber = "C2",
        productName = "P1"
      ),
      ZuoraInvoiceItem(
        subscriptionNumber = "S1",
        serviceStartDate = LocalDate.of(2022, 7, 1),
        chargeNumber = "C3",
        productName = "P1"
      ),
      ZuoraInvoiceItem(
        subscriptionNumber = "S1",
        serviceStartDate = LocalDate.of(2023, 7, 1),
        chargeNumber = "C4",
        productName = "P1"
      ),
      ZuoraInvoiceItem(
        subscriptionNumber = "S1",
        serviceStartDate = LocalDate.of(2023, 8, 1),
        chargeNumber = "C2",
        productName = "P1"
      ),
      ZuoraInvoiceItem(
        subscriptionNumber = "S1",
        serviceStartDate = LocalDate.of(2023, 9, 1),
        chargeNumber = "C1",
        productName = "P1"
      )
    )
  )

  private val migrationStartDate2022 = LocalDate.of(2022, 11, 14)
  private val testTime1 = OffsetDateTime.of(LocalDateTime.of(2022, 7, 10, 10, 2), ZoneOffset.ofHours(0)).toInstant

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("estimate")(
    test("Start date is correct for subscription less than one year old (1)") {
      val invoiceList = invoiceListFromJson("NewspaperDelivery/Sixday+/InvoicePreview.json")
      val subscription = subscriptionFromJson("NewspaperDelivery/Sixday+/Subscription.json")
      val expectedStartDate = LocalDate.of(2022, 12, 14)

      for {
        _ <- TestClock.setTime(testTime1)
        startDate <- spreadEarliestStartDate(subscription, invoiceList, migrationStartDate2022)
      } yield assert(startDate)(equalTo(expectedStartDate))
    },
    test("Start date is correct for subscription less than one year old (2)") {
      val invoiceList = invoiceListFromJson("NewspaperDelivery/Waitrose25%Discount/InvoicePreview.json")
      val subscription = subscriptionFromJson("NewspaperDelivery/Waitrose25%Discount/Subscription.json")
      val expectedStartDate = LocalDate.of(2023, 3, 14)

      for {
        _ <- TestClock.setTime(testTime1)
        startDate <- spreadEarliestStartDate(subscription, invoiceList, migrationStartDate2022)
      } yield assert(startDate)(equalTo(expectedStartDate))
    },
    test("Start date is correct for subscription less than one year old (3)") {
      val invoiceList = invoiceListFromJson("NewspaperDelivery/Everyday/InvoicePreview.json")
      val subscription = subscriptionFromJson("NewspaperDelivery/Everyday/Subscription.json")
      val expectedStartDate = LocalDate.of(2022, 11, 14)

      for {
        _ <- TestClock.setTime(testTime1)
        startDate <- spreadEarliestStartDate(subscription, invoiceList, migrationStartDate2022)
      } yield assert(startDate)(equalTo(expectedStartDate))
    },
    test("updates cohort table with EstimationComplete when data is complete") {
      val productCatalogue = ZuoraProductCatalogue(products =
        Set(
          ZuoraProduct(
            "P2",
            Set(
              ZuoraProductRatePlan(
                id = "PRP1",
                name = "RP1",
                status = "Active",
                productRatePlanCharges = Set(
                  ZuoraProductRatePlanCharge(
                    id = "PRPC1",
                    billingPeriod = Some("Month"),
                    pricing = Set(ZuoraPricing(currency = "GBP", price = Some(BigDecimal("1.30"))))
                  ),
                  ZuoraProductRatePlanCharge(
                    id = "PRPC4",
                    billingPeriod = Some("Month"),
                    pricing = Set(ZuoraPricing(currency = "GBP", price = Some(BigDecimal("1.38"))))
                  )
                )
              )
            )
          )
        )
      )
      val cohortItemRead = CohortItem(
        subscriptionName = "S1",
        processingStage = ReadyForEstimation
      )
      val cohortItemExpectedToWrite = CohortItem(
        subscriptionName = "S1",
        processingStage = EstimationComplete,
        startDate = Some(LocalDate.of(2023, 7, 1)),
        currency = Some("GBP"),
        oldPrice = Some(1.24),
        estimatedNewPrice = Some(1.38),
        billingPeriod = Some("Month"),
        whenEstimationDone = Some(time)
      )
      val expectedSubscriptionFetch = MockZuora.FetchSubscription(
        assertion = equalTo("S1"),
        result = value(subscription2)
      )

      val expectedAccountToFetch = MockZuora.FetchAccount(
        assertion = equalTo(("A9107", "S1")),
        result = value(account)
      )
      val expectedInvoiceFetch = MockZuora.FetchInvoicePreview(
        assertion = equalTo("A11", LocalDate.of(2023, 9, 1)),
        result = value(invoicePreview)
      )
      val expectedZuoraUse = expectedSubscriptionFetch and expectedInvoiceFetch and expectedAccountToFetch
      val expectedCohortTableUpdate = MockCohortTable.Update(
        assertion = equalTo(cohortItemExpectedToWrite),
        result = unit
      )
      for {
        _ <- TestClock.setTime(time)
        _ <- EstimationHandler
          .estimate(productCatalogue, earliestStartDate = LocalDate.of(2022, 5, 1))(cohortItemRead)
          .provide(expectedZuoraUse, expectedCohortTableUpdate)
      } yield assertTrue(true)
    },
    test("updates cohort table with NoPriceIncrease when estimated new price <= old price") {
      val productCatalogue = ZuoraProductCatalogue(products =
        Set(
          ZuoraProduct(
            "P1",
            Set(
              ZuoraProductRatePlan(
                id = "PRP1",
                name = "RP1",
                status = "Active",
                productRatePlanCharges = Set(
                  ZuoraProductRatePlanCharge(
                    id = "PRPC1",
                    billingPeriod = Some("Month"),
                    pricing = Set(ZuoraPricing(currency = "GBP", price = Some(BigDecimal("1.15"))))
                  ),
                  ZuoraProductRatePlanCharge(
                    id = "PRPC4",
                    billingPeriod = Some("Month"),
                    pricing = Set(ZuoraPricing(currency = "GBP", price = Some(BigDecimal("1.16"))))
                  )
                )
              )
            )
          )
        )
      )
      val cohortItemRead = CohortItem(
        subscriptionName = "S1",
        processingStage = ReadyForEstimation
      )
      val expectedSubscriptionFetch = MockZuora.FetchSubscription(
        assertion = equalTo("S1"),
        result = value(subscription)
      )
      val expectedAccountToFetch = MockZuora.FetchAccount(
        assertion = equalTo(("A9107", "S1")),
        result = value(account)
      )
      val expectedInvoiceFetch = MockZuora.FetchInvoicePreview(
        assertion = equalTo("A11", LocalDate.of(2023, 9, 1)),
        result = value(invoicePreview)
      )
      val expectedZuoraUse = expectedSubscriptionFetch and expectedInvoiceFetch and expectedAccountToFetch
      val cohortItemExpectedToWrite = CohortItem(
        subscriptionName = "S1",
        processingStage = NoPriceIncrease,
        startDate = Some(LocalDate.of(2023, 7, 1)),
        currency = Some("GBP"),
        oldPrice = Some(1.24),
        estimatedNewPrice = Some(1.16),
        billingPeriod = Some("Month"),
        whenEstimationDone = Some(time)
      )
      val expectedCohortTableUpdate = MockCohortTable.Update(
        assertion = equalTo(cohortItemExpectedToWrite),
        result = unit
      )
      for {
        _ <- TestClock.setTime(time)
        _ <- EstimationHandler
          .estimate(productCatalogue, earliestStartDate = LocalDate.of(2022, 5, 1))(cohortItemRead)
          .provide(expectedZuoraUse, expectedCohortTableUpdate)
      } yield assertTrue(true)
    },
    test("updates cohort table with CappedPriceIncrease when estimated new price is 20% or more above the old price") {
      val productCatalogue = ZuoraProductCatalogue(products =
        Set(
          ZuoraProduct(
            "P1",
            Set(
              ZuoraProductRatePlan(
                id = "PRP1",
                name = "RP1",
                status = "Active",
                productRatePlanCharges = Set(
                  ZuoraProductRatePlanCharge(
                    id = "PRPC1",
                    billingPeriod = Some("Month"),
                    pricing = Set(ZuoraPricing(currency = "GBP", price = Some(BigDecimal("1.15"))))
                  ),
                  ZuoraProductRatePlanCharge(
                    id = "PRPC4",
                    billingPeriod = Some("Month"),
                    pricing = Set(ZuoraPricing(currency = "GBP", price = Some(BigDecimal("21.17"))))
                  )
                )
              )
            )
          )
        )
      )
      val cohortItemRead = CohortItem(
        subscriptionName = "S1",
        processingStage = ReadyForEstimation
      )
      val expectedSubscriptionFetch = MockZuora.FetchSubscription(
        assertion = equalTo("S1"),
        result = value(subscription)
      )
      val expectedAccountToFetch = MockZuora.FetchAccount(
        assertion = equalTo(("A9107", "S1")),
        result = value(account)
      )
      val expectedInvoiceFetch = MockZuora.FetchInvoicePreview(
        assertion = equalTo("A11", LocalDate.of(2023, 9, 1)),
        result = value(invoicePreview)
      )
      val expectedZuoraUse = expectedSubscriptionFetch and expectedInvoiceFetch and expectedAccountToFetch
      val cohortItemExpectedToWrite = CohortItem(
        subscriptionName = "S1",
        processingStage = CappedPriceIncrease,
        startDate = Some(LocalDate.of(2023, 7, 1)),
        currency = Some("GBP"),
        oldPrice = Some(1.24),
        estimatedNewPrice = Some(21.17),
        billingPeriod = Some("Month"),
        whenEstimationDone = Some(time)
      )
      val expectedCohortTableUpdate = MockCohortTable.Update(
        assertion = equalTo(cohortItemExpectedToWrite),
        result = unit
      )
      for {
        _ <- TestClock.setTime(time)
        _ <- EstimationHandler
          .estimate(productCatalogue, earliestStartDate = LocalDate.of(2022, 5, 1))(cohortItemRead)
          .provide(expectedZuoraUse, expectedCohortTableUpdate)
      } yield assertTrue(true)
    }
  )
}
