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

  private val subscriptionName = "Sub-0001"
  private val startDate = LocalDate.of(2020, 1, 1)
  private val currency = "GBP"
  private val oldPrice = BigDecimal(10.00)

  private val estimatedNewPrice = BigDecimal(15.00)
  test("For legacy migrations, we need the estimatedNewPrice to be higher than the capped price") {
    assert(PriceCap.priceCapLegacy(oldPrice, estimatedNewPrice) < estimatedNewPrice)
  }

  private val currentTime = Instant.parse("2020-05-21T15:16:37Z")

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
      subscriptionName = subscriptionName,
      processingStage = EstimationComplete,
      startDate = Some(startDate),
      currency = Some(currency),
      oldPrice = Some(oldPrice),
      estimatedNewPrice = Some(estimatedNewPrice)
    )

    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)

    val cohortSpec = CohortSpec("Name", "Campaign1", LocalDate.of(2000, 1, 1), startDate)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(currentTime)
          program <- SalesforcePriceRiseCreationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(createdPriceRises.size, 1)
    assertEquals(createdPriceRises(0).Name, Some(subscriptionName))
    assertEquals(createdPriceRises(0).SF_Subscription__c, Some(s"SubscritionId-$subscriptionName"))
    assertEquals(createdPriceRises(0).Buyer__c, Some(s"Buyer-$subscriptionName"))
    assertEquals(createdPriceRises(0).Current_Price_Today__c, Some(oldPrice))

    // Cohort spec name "Name", causes a Legacy migration
    assertEquals(
      createdPriceRises(0).Guardian_Weekly_New_Price__c,
      Some(PriceCap.priceCapLegacy(oldPrice, estimatedNewPrice))
    )
    assertEquals(createdPriceRises(0).Price_Rise_Date__c, Some(startDate))

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
      Some(s"SubscritionId-$subscriptionName-price-rise-id")
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).whenSfShowEstimate,
      Some(currentTime)
    )
  }

  test(
    "SalesforcePriceRiseCreateHandler should get estimate from cohort table and create sf price rise (in the case of a non capped price rise: membership Batch1)"
  ) {
    // In this case
    val createdPriceRises = ArrayBuffer[SalesforcePriceRise]()
    val updatedPriceRises = ArrayBuffer[SalesforcePriceRise]()
    val stubSalesforceClient = stubSFClient(createdPriceRises, updatedPriceRises)
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()

    val cohortItem = CohortItem(
      subscriptionName = subscriptionName,
      processingStage = EstimationComplete,
      startDate = Some(startDate),
      currency = Some(currency),
      oldPrice = Some(oldPrice),
      estimatedNewPrice = Some(estimatedNewPrice)
    )

    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)

    // In this case the name of the cohort will trigger a non price cap
    val cohortSpec = CohortSpec("Membership2023_Batch1", "Campaign1", LocalDate.of(2000, 1, 1), startDate)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(currentTime)
          program <- SalesforcePriceRiseCreationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(createdPriceRises.size, 1)
    assertEquals(createdPriceRises(0).Name, Some(subscriptionName))
    assertEquals(createdPriceRises(0).SF_Subscription__c, Some(s"SubscritionId-$subscriptionName"))
    assertEquals(createdPriceRises(0).Buyer__c, Some(s"Buyer-$subscriptionName"))
    assertEquals(createdPriceRises(0).Current_Price_Today__c, Some(oldPrice))
    assertEquals(
      createdPriceRises(0).Guardian_Weekly_New_Price__c,
      Some(estimatedNewPrice) // expecting a non capped estimated price in this case
    )
    assertEquals(createdPriceRises(0).Price_Rise_Date__c, Some(startDate))

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
      Some(s"SubscritionId-$subscriptionName-price-rise-id")
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).whenSfShowEstimate,
      Some(currentTime)
    )
  }

  test(
    "SalesforcePriceRiseCreateHandler should get estimate from cohort table and create sf price rise (in the case of a non capped price rise: membership Batch2)"
  ) {
    // In this case
    val createdPriceRises = ArrayBuffer[SalesforcePriceRise]()
    val updatedPriceRises = ArrayBuffer[SalesforcePriceRise]()
    val stubSalesforceClient = stubSFClient(createdPriceRises, updatedPriceRises)
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()

    val cohortItem = CohortItem(
      subscriptionName = subscriptionName,
      processingStage = EstimationComplete,
      startDate = Some(startDate),
      currency = Some(currency),
      oldPrice = Some(oldPrice),
      estimatedNewPrice = Some(estimatedNewPrice)
    )

    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)

    // In this case the name of the cohort will trigger a non price cap
    val cohortSpec = CohortSpec("Membership2023_Batch2", "Campaign1", LocalDate.of(2000, 1, 1), startDate)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(currentTime)
          program <- SalesforcePriceRiseCreationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(createdPriceRises.size, 1)
    assertEquals(createdPriceRises(0).Name, Some(subscriptionName))
    assertEquals(createdPriceRises(0).SF_Subscription__c, Some(s"SubscritionId-$subscriptionName"))
    assertEquals(createdPriceRises(0).Buyer__c, Some(s"Buyer-$subscriptionName"))
    assertEquals(createdPriceRises(0).Current_Price_Today__c, Some(oldPrice))
    assertEquals(
      createdPriceRises(0).Guardian_Weekly_New_Price__c,
      Some(estimatedNewPrice) // expecting a non capped estimated price in this case
    )
    assertEquals(createdPriceRises(0).Price_Rise_Date__c, Some(startDate))

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
      Some(s"SubscritionId-$subscriptionName-price-rise-id")
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).whenSfShowEstimate,
      Some(currentTime)
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
          subscriptionName = subscriptionName,
          processingStage = EstimationComplete,
          startDate = Some(startDate),
          currency = Some(currency),
          oldPrice = Some(oldPrice),
          estimatedNewPrice = Some(estimatedNewPrice),
          salesforcePriceRiseId = Some("existing-price-rise-id")
        )
      )

    val cohortSpec = CohortSpec("Name", "Campaign1", LocalDate.of(2000, 1, 1), startDate)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(currentTime)
          program <- SalesforcePriceRiseCreationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(updatedPriceRises.size, 1)
    assertEquals(updatedPriceRises(0).SF_Subscription__c, Some(s"SubscritionId-$subscriptionName"))
    assertEquals(updatedPriceRises(0).Buyer__c, Some(s"Buyer-$subscriptionName"))
    assertEquals(updatedPriceRises(0).Current_Price_Today__c, Some(oldPrice))

    // Cohort spec name "Name", causes a Legacy migration
    assertEquals(
      updatedPriceRises(0).Guardian_Weekly_New_Price__c,
      Some(PriceCap.priceCapLegacy(oldPrice, estimatedNewPrice))
    )
    assertEquals(updatedPriceRises(0).Price_Rise_Date__c, Some(startDate))

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
      Some(currentTime)
    )
  }
}
