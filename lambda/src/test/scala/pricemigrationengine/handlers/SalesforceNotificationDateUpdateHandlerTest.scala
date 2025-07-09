package pricemigrationengine.handlers

import pricemigrationengine.TestLogging
import pricemigrationengine.model.CohortTableFilter.{NotificationSendComplete, NotificationSendDateWrittenToSalesforce}
import pricemigrationengine.model._
import pricemigrationengine.services._
import pricemigrationengine.libs.Runner.unsafeRunSync
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.ZStream
import zio.test.{TestClock, testEnvironment}
import zio.{IO, ZIO, ZLayer}

import java.time.{Instant, LocalDate, ZoneOffset}
import scala.collection.mutable.ArrayBuffer

class SalesforceNotificationDateUpdateHandlerTest extends munit.FunSuite {
  private val subscriptionName = "Sub-0001"
  private val whenNotificationSentDate = LocalDate.of(2020, 3, 23)
  private val priceRiseId = "price-rise-id"
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
          assertEquals(filter, NotificationSendComplete)
          ZStream(cohortItem)
        }

        override def create(cohortItem: CohortItem): ZIO[Any, Failure, Unit] = ???

        override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
          updatedResultsWrittenToCohortTable.addOne(result)
          ZIO.succeed(())
        }

        override def fetchAll(): ZStream[Any, CohortFetchFailure, CohortItem] = ???
      }
    )
  }

  private def stubSFClient(
      updatedPriceRises: ArrayBuffer[SalesforcePriceRise]
  ) = {
    ZLayer.succeed(
      new SalesforceClient {

        override def getSubscriptionByName(
            subscriptionName: String
        ): IO[SalesforceClientFailure, SalesforceSubscription] = ???

        override def createPriceRise(
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResponse] = ???

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

  test("SalesforceNotificationDateUpdateHandler should write whenNotificationSentWrittenToSalesforce to salesforce") {
    val updatedPriceRises = ArrayBuffer[SalesforcePriceRise]()
    val stubSalesforceClient = stubSFClient(updatedPriceRises)
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()

    val cohortSpec: CohortSpec =
      CohortSpec("cohortName", "brazeName", LocalDate.of(2022, 1, 1), None)

    val cohortItem = CohortItem(
      subscriptionName = subscriptionName,
      processingStage = NotificationSendComplete,
      salesforcePriceRiseId = Some(priceRiseId),
      whenNotificationSent = Some(whenNotificationSentDate.atStartOfDay().toInstant(ZoneOffset.UTC))
    )

    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(currentTime)
          program <- SalesforceNotificationDateUpdateHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(updatedPriceRises.size, 1)
    assertEquals(updatedPriceRises(0).Name, None)
    assertEquals(updatedPriceRises(0).SF_Subscription__c, None)
    assertEquals(updatedPriceRises(0).Buyer__c, None)
    assertEquals(updatedPriceRises(0).Current_Price_Today__c, None)
    assertEquals(updatedPriceRises(0).Guardian_Weekly_New_Price__c, None)
    assertEquals(updatedPriceRises(0).Price_Rise_Date__c, None)
    assertEquals(updatedPriceRises(0).Date_Letter_Sent__c, Some(whenNotificationSentDate))

    assertEquals(updatedResultsWrittenToCohortTable.size, 1)
    assertEquals(
      updatedResultsWrittenToCohortTable(0).subscriptionName,
      s"Sub-0001"
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).processingStage,
      NotificationSendDateWrittenToSalesforce
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).whenNotificationSentWrittenToSalesforce,
      Some(currentTime)
    )
  }
}
