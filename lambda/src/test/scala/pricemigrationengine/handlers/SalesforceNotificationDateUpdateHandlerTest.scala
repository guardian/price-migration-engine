package pricemigrationengine.handlers

import java.time.{LocalDate, ZoneOffset}

import pricemigrationengine.model.CohortTableFilter.{EmailSendComplete, EmailSendDateWrittenToSalesforce}
import pricemigrationengine.model._
import pricemigrationengine.services._
import pricemigrationengine.{StubClock, TestLogging}
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer}

import scala.collection.mutable.ArrayBuffer

class SalesforceNotificationDateUpdateHandlerTest extends munit.FunSuite {
  val expectedSubscriptionName = "Sub-0001"
  val expectedWhenEmailSentDate = LocalDate.of(2020,3,23)
  val expectedPriceRiseId = "price-rise-id"

  def createStubCohortTable(updatedResultsWrittenToCohortTable:ArrayBuffer[CohortItem], cohortItem: CohortItem) = {
    ZLayer.succeed(
      new CohortTable.Service {
        override def fetch(
          filter: CohortTableFilter,
          beforeDateInclusive: Option[LocalDate]
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          assertEquals(filter, EmailSendComplete)
          IO.succeed(ZStream(cohortItem))
        }

        override def put(cohortItem: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = ???

        override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
          updatedResultsWrittenToCohortTable.addOne(result)
          IO.succeed(())
        }
      }
    )
  }

  private def stubSFClient(
    updatedPriceRises: ArrayBuffer[SalesforcePriceRise]
  ) = {
    ZLayer.succeed(
      new SalesforceClient.Service {
        override def getSubscriptionByName(
            subscriptionName: String
        ): IO[SalesforceClientFailure, SalesforceSubscription] = ???

        override def createPriceRise(
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResponse] = ???

        override def updatePriceRise(
            priceRiseId: String, priceRise: SalesforcePriceRise
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

  test("SalesforceNotificationDateUpdateHandler should write whenEmailSentWrittenToSalesforce to salesforce") {
    val updatedPriceRises = ArrayBuffer[SalesforcePriceRise]()
    val stubSalesforceClient = stubSFClient(updatedPriceRises)
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()

    val cohortItem = CohortItem(
      subscriptionName = expectedSubscriptionName,
      processingStage = EmailSendComplete,
      salesforcePriceRiseId = Some(expectedPriceRiseId),
      whenEmailSent = Some(expectedWhenEmailSentDate.atStartOfDay().toInstant(ZoneOffset.UTC))
    )

    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)

    assertEquals(
      default.unsafeRunSync(
        SalesforceNotificationDateUpdateHandler.main
          .provideLayer(
            TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ StubClock.clock
          )
      ),
      Success(())
    )

    assertEquals(updatedPriceRises.size, 1)
    assertEquals(updatedPriceRises(0).Name, None)
    assertEquals(updatedPriceRises(0).SF_Subscription__c, None)
    assertEquals(updatedPriceRises(0).Buyer__c, None)
    assertEquals(updatedPriceRises(0).Current_Price_Today__c, None)
    assertEquals(updatedPriceRises(0).Guardian_Weekly_New_Price__c, None)
    assertEquals(updatedPriceRises(0).Price_Rise_Date__c, None)
    assertEquals(updatedPriceRises(0).Date_Letter_Sent__c, Some(expectedWhenEmailSentDate))

    assertEquals(updatedResultsWrittenToCohortTable.size, 1)
    assertEquals(
      updatedResultsWrittenToCohortTable(0).subscriptionName,
      s"Sub-0001"
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).processingStage,
      EmailSendDateWrittenToSalesforce
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).whenEmailSentWrittenToSalesforce,
      Some(StubClock.expectedCurrentTime)
    )
  }
}
