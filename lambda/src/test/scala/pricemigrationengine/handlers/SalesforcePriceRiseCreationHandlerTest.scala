package pricemigrationengine.handlers

import pricemigrationengine.StubClock.withStubClock

import java.time.LocalDate
import pricemigrationengine.model.CohortTableFilter.{EstimationComplete, SalesforcePriceRiceCreationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services._
import pricemigrationengine.{StubClock, TestLogging}
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer}

import scala.collection.mutable.ArrayBuffer

class SalesforcePriceRiseCreationHandlerTest extends munit.FunSuite {

  val expectedSubscriptionName = "Sub-0001"
  val expectedStartDate = LocalDate.of(2020, 1, 1)
  val expectedCurrency = "GBP"
  val expectedOldPrice = BigDecimal(11.11)
  val expectedEstimatedNewPrice = BigDecimal(22.22)

  private val expectedHandlerOutput = HandlerOutput(
    isComplete = true
  )

  def createStubCohortTable(updatedResultsWrittenToCohortTable: ArrayBuffer[CohortItem], cohortItem: CohortItem) = {
    ZLayer.succeed(
      new CohortTable.Service {
        override def fetch(
            filter: CohortTableFilter,
            beforeDateInclusive: Option[LocalDate]
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          assertEquals(filter, EstimationComplete)
          IO.succeed(ZStream(cohortItem))
        }

        override def create(cohortItem: CohortItem): ZIO[Any, Failure, Unit] = ???

        override def fetchAll(): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = ???

        override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
          updatedResultsWrittenToCohortTable.addOne(result)
          IO.succeed(())
        }
      }
    )
  }

  private def stubSFClient(
      createdPriceRises: ArrayBuffer[SalesforcePriceRise],
      updatedPriceRises: ArrayBuffer[SalesforcePriceRise]
  ) = {
    ZLayer.succeed(
      new SalesforceClient.Service {
        override def getSubscriptionByName(
            subscriptionName: String
        ): IO[SalesforceClientFailure, SalesforceSubscription] = {
          IO.attempt(
            SalesforceSubscription(
              s"SubscritionId-$subscriptionName",
              subscriptionName,
              s"Buyer-$subscriptionName",
              "Active",
              Some("Newspaper - Digital Voucher")
            )
          ).orElseFail(SalesforceClientFailure(""))
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
      default.unsafeRunSync(
        withStubClock(
          SalesforcePriceRiseCreationHandler.main
            .provideLayer(
              TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient
            )
        )
      ),
      Success(expectedHandlerOutput)
    )

    assertEquals(createdPriceRises.size, 1)
    assertEquals(createdPriceRises(0).Name, Some(expectedSubscriptionName))
    assertEquals(createdPriceRises(0).SF_Subscription__c, Some(s"SubscritionId-$expectedSubscriptionName"))
    assertEquals(createdPriceRises(0).Buyer__c, Some(s"Buyer-$expectedSubscriptionName"))
    assertEquals(createdPriceRises(0).Current_Price_Today__c, Some(expectedOldPrice))
    assertEquals(createdPriceRises(0).Guardian_Weekly_New_Price__c, Some(expectedEstimatedNewPrice))
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
      Some(StubClock.expectedCurrentTime)
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
      default.unsafeRunSync(
        withStubClock(
          SalesforcePriceRiseCreationHandler.main
            .provideLayer(
              TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient
            )
        )
      ),
      Success(expectedHandlerOutput)
    )

    assertEquals(updatedPriceRises.size, 1)
    assertEquals(updatedPriceRises(0).SF_Subscription__c, Some(s"SubscritionId-$expectedSubscriptionName"))
    assertEquals(updatedPriceRises(0).Buyer__c, Some(s"Buyer-$expectedSubscriptionName"))
    assertEquals(updatedPriceRises(0).Current_Price_Today__c, Some(expectedOldPrice))
    assertEquals(updatedPriceRises(0).Guardian_Weekly_New_Price__c, Some(expectedEstimatedNewPrice))
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
      Some(StubClock.expectedCurrentTime)
    )
  }
}
