package pricemigrationengine.handlers

import pricemigrationengine.TestLogging
import pricemigrationengine.model.CohortTableFilter.{EstimationComplete, SalesforcePriceRiceCreationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services._
import pricemigrationengine.util.Runner.unsafeRunSync
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.ZStream
import zio.test.{TestClock, testEnvironment}
import zio.{IO, ZIO, ZLayer}

import java.time.{Instant, LocalDate}
import scala.collection.mutable.ArrayBuffer

class SalesforcePriceRiseCreationHandlerTest extends munit.FunSuite {

  private val expectedSubscriptionName = "Sub-0001"
  private val expectedStartDate = LocalDate.of(2020, 1, 1)
  private val expectedCurrency = "GBP"
  private val expectedOldPrice = BigDecimal(10.00)
  private val expectedEstimatedNewPrice = BigDecimal(15.00)
  private val expectedCurrentTime = Instant.parse("2020-05-21T15:16:37Z")

  private def createStubCohortTable(
      updatedResultsWrittenToCohortTable: ArrayBuffer[CohortItem],
      cohortItem: CohortItem
  ) = {
    ZLayer.succeed(
      new CohortTable {

        override def fetch(
            filter: CohortTableFilter,
            beforeDateInclusive: Option[LocalDate]
        ): ZStream[Any, CohortFetchFailure, CohortItem] = {
          assertEquals(filter, EstimationComplete)
          ZStream(cohortItem)
        }

        override def create(cohortItem: CohortItem): ZIO[Any, Failure, Unit] = ???

        override def fetchAll(): ZStream[Any, CohortFetchFailure, CohortItem] = ???

        override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
          updatedResultsWrittenToCohortTable.addOne(result)
          ZIO.succeed(())
        }
      }
    )
  }

  private def stubSFClient(
      createdPriceRises: ArrayBuffer[SalesforcePriceRise],
      updatedPriceRises: ArrayBuffer[SalesforcePriceRise]
  ) = {
    ZLayer.succeed(
      new SalesforceClient {

        override def getSubscriptionByName(
            subscriptionName: String
        ): IO[SalesforceClientFailure, SalesforceSubscription] = {
          ZIO
            .attempt(
              SalesforceSubscription(
                s"SubscritionId-$subscriptionName",
                subscriptionName,
                s"Buyer-$subscriptionName",
                "Active",
                Some("Newspaper - Digital Voucher")
              )
            )
            .orElseFail(SalesforceClientFailure(""))
        }

        override def createPriceRise(
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResponse] = {
          createdPriceRises.addOne(priceRise)
          ZIO.succeed(
            SalesforcePriceRiseCreationResponse(s"${priceRise.SF_Subscription__c.getOrElse("none")}-price-rise-id")
          )
        }

        override def updatePriceRise(
            priceRiseId: String,
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, Unit] = {
          updatedPriceRises.addOne(priceRise)
          ZIO.unit
        }

        override def getContact(
            contactId: String
        ): IO[SalesforceClientFailure, SalesforceContact] = ???
      }
    )
  }

  test("SalesforcePriceRiseCreateHandler should get estimate from cohort table and create sf price rise") {
    val createdPriceRises = ArrayBuffer[SalesforcePriceRise]()
    val updatedPriceRises = ArrayBuffer[SalesforcePriceRise]()
    val stubSalesforceClient = stubSFClient(createdPriceRises, updatedPriceRises)
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()

    val cohortItem = CohortItem(
      subscriptionName = expectedSubscriptionName,
      processingStage = EstimationComplete,
      startDate = Some(expectedStartDate),
      currency = Some(expectedCurrency),
      oldPrice = Some(expectedOldPrice),
      estimatedNewPrice = Some(expectedEstimatedNewPrice)
    )

    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- SalesforcePriceRiseCreationHandler.main
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(createdPriceRises.size, 1)
    assertEquals(createdPriceRises(0).Name, Some(expectedSubscriptionName))
    assertEquals(createdPriceRises(0).SF_Subscription__c, Some(s"SubscritionId-$expectedSubscriptionName"))
    assertEquals(createdPriceRises(0).Buyer__c, Some(s"Buyer-$expectedSubscriptionName"))
    assertEquals(createdPriceRises(0).Current_Price_Today__c, Some(expectedOldPrice))
    assertEquals(
      createdPriceRises(0).Guardian_Weekly_New_Price__c,
      Some(List(expectedOldPrice * 1.2, expectedEstimatedNewPrice).min)
    )
    assertEquals(createdPriceRises(0).Price_Rise_Date__c, Some(expectedStartDate))

    assertEquals(updatedResultsWrittenToCohortTable.size, 1)
    assertEquals(
      updatedResultsWrittenToCohortTable(0).subscriptionName,
      s"Sub-0001"
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).processingStage,
      SalesforcePriceRiceCreationComplete
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).salesforcePriceRiseId,
      Some(s"SubscritionId-$expectedSubscriptionName-price-rise-id")
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).whenSfShowEstimate,
      Some(expectedCurrentTime)
    )
  }

  test(
    "SalesforcePriceRiseCreateHandler should get estimate from cohort table and update sf price rise if it exists"
  ) {
    val createdPriceRises = ArrayBuffer[SalesforcePriceRise]()
    val updatedPriceRises = ArrayBuffer[SalesforcePriceRise]()
    val stubSalesforceClient = stubSFClient(createdPriceRises, updatedPriceRises)
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()

    val stubCohortTable =
      createStubCohortTable(
        updatedResultsWrittenToCohortTable,
        CohortItem(
          subscriptionName = expectedSubscriptionName,
          processingStage = EstimationComplete,
          startDate = Some(expectedStartDate),
          currency = Some(expectedCurrency),
          oldPrice = Some(expectedOldPrice),
          estimatedNewPrice = Some(expectedEstimatedNewPrice),
          salesforcePriceRiseId = Some("existing-price-rise-id")
        )
      )

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- SalesforcePriceRiseCreationHandler.main
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(updatedPriceRises.size, 1)
    assertEquals(updatedPriceRises(0).SF_Subscription__c, Some(s"SubscritionId-$expectedSubscriptionName"))
    assertEquals(updatedPriceRises(0).Buyer__c, Some(s"Buyer-$expectedSubscriptionName"))
    assertEquals(updatedPriceRises(0).Current_Price_Today__c, Some(expectedOldPrice))
    assertEquals(
      updatedPriceRises(0).Guardian_Weekly_New_Price__c,
      Some(List(expectedOldPrice * 1.2, expectedEstimatedNewPrice).min)
    )
    assertEquals(updatedPriceRises(0).Price_Rise_Date__c, Some(expectedStartDate))

    assertEquals(updatedResultsWrittenToCohortTable.size, 1)
    assertEquals(
      updatedResultsWrittenToCohortTable(0).subscriptionName,
      s"Sub-0001"
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).processingStage,
      SalesforcePriceRiceCreationComplete
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).salesforcePriceRiseId,
      None
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).whenSfShowEstimate,
      Some(expectedCurrentTime)
    )
  }
}
