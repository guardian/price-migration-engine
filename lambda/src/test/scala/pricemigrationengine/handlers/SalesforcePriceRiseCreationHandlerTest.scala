package pricemigrationengine.handlers

import pricemigrationengine.TestLogging
import pricemigrationengine.model.CohortTableFilter.{EstimationComplete, SalesforcePriceRiseCreationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services._
import pricemigrationengine.libs.Runner.unsafeRunSync
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
}
