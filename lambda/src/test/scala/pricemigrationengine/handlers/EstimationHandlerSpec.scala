package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.{EstimationComplete, NoPriceIncrease, ReadyForEstimation}
import pricemigrationengine.model._
import pricemigrationengine.service.{MockCohortTable, MockZuora}
import zio._
import zio.mock.Expectation._
import zio.test.Assertion._
import zio.test._

import java.time._

object EstimationHandlerSpec extends ZIOSpecDefault {

  private val time = OffsetDateTime.of(LocalDateTime.of(2022, 5, 16, 10, 2), ZoneOffset.ofHours(0))

  private val subscription = ZuoraSubscription(
    subscriptionNumber = "S1",
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
          )
        )
      )
    ),
    accountNumber = "A1",
    accountId = "A11",
    status = "Active",
    termStartDate = LocalDate.of(2022, 1, 1),
    termEndDate = LocalDate.of(2023, 1, 1)
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
      )
    )
  )

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("estimate")(
    test("updates cohort table with EstimationComplete when data is complete") {
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
                    pricing = Set(ZuoraPricing(currency = "GBP", price = Some(BigDecimal("2.45"))))
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
        startDate = Some(LocalDate.of(2022, 7, 1)),
        currency = Some("GBP"),
        oldPrice = Some(1.23),
        estimatedNewPrice = Some(2.45),
        billingPeriod = Some("Month"),
        whenEstimationDone = Some(Instant.from(time))
      )
      val expectedSubscriptionFetch = MockZuora.FetchSubscription(
        assertion = equalTo("S1"),
        result = value(subscription)
      )
      val expectedInvoiceFetch = MockZuora.FetchInvoicePreview(
        assertion = equalTo("A11", LocalDate.of(2023, 6, 1)),
        result = value(invoicePreview)
      )
      val expectedZuoraUse = expectedSubscriptionFetch and expectedInvoiceFetch
      val expectedCohortTableUpdate = MockCohortTable.Update(
        assertion = equalTo(cohortItemExpectedToWrite),
        result = unit
      )
      for {
        _ <- TestClock.setDateTime(time)
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
      val expectedInvoiceFetch = MockZuora.FetchInvoicePreview(
        assertion = equalTo("A11", LocalDate.of(2023, 6, 1)),
        result = value(invoicePreview)
      )
      val expectedZuoraUse = expectedSubscriptionFetch and expectedInvoiceFetch
      val cohortItemExpectedToWrite = CohortItem(
        subscriptionName = "S1",
        processingStage = NoPriceIncrease,
        startDate = Some(LocalDate.of(2022, 7, 1)),
        currency = Some("GBP"),
        oldPrice = Some(1.23),
        estimatedNewPrice = Some(1.15),
        billingPeriod = Some("Month"),
        whenEstimationDone = Some(Instant.from(time))
      )
      val expectedCohortTableUpdate = MockCohortTable.Update(
        assertion = equalTo(cohortItemExpectedToWrite),
        result = unit
      )
      for {
        _ <- TestClock.setDateTime(time)
        _ <- EstimationHandler
          .estimate(productCatalogue, earliestStartDate = LocalDate.of(2022, 5, 1))(cohortItemRead)
          .provide(expectedZuoraUse, expectedCohortTableUpdate)
      } yield assertTrue(true)
    }
  )
}
