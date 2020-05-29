package pricemigrationengine.handlers

import java.time.{DateTimeException, Instant, LocalDate, OffsetDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

import pricemigrationengine.model.CohortTableFilter.EstimationComplete
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.Exit.Success
import zio.Runtime.default
import zio.clock.Clock
import zio.duration.Duration
import zio.stream.ZStream
import zio.{IO, UIO, ZIO, ZLayer, console}

import scala.collection.mutable.ArrayBuffer

class SalesforcePriceRiseCreationHandlerTest extends munit.FunSuite {
  test("SalesforcePriceRiseCreateHandler should get estimated prices from ") {
    val stubConfiguration = ZLayer.succeed(
      new AmendmentConfiguration.Service {
        override val config: IO[ConfigurationFailure, AmendmentConfig] =
          IO.succeed(AmendmentConfig(LocalDate.now))
      }
    )

    val expectedCurrentTime = Instant.parse("2020-05-21T15:16:37Z")
    val stubClock = ZLayer.succeed(
      new Clock.Service {
        override def currentTime(unit: TimeUnit): UIO[Long] = ???
        override def currentDateTime: IO[DateTimeException, OffsetDateTime] =
          IO.succeed(expectedCurrentTime.atOffset(ZoneOffset.of("-08:00")))
        override def nanoTime: UIO[Long] = ???
        override def sleep(duration: Duration): UIO[Unit] = ???
      }
    )
    val stubLogging = console.Console.live >>> ConsoleLogging.impl

    val expectedSubscriptionName = "Sub-0001"
    val expectedStartDate = LocalDate.of(2020, 1, 1)
    val expectedCurrency = "GBP"
    val expectedOldPrice = BigDecimal(11.11)
    val expectedEstimatedNewPrice = BigDecimal(22.22)

    val updatedResultsWrittenToCohortTable = ArrayBuffer[SalesforcePriceRiseCreationDetails]()

    val stubCohortTable = ZLayer.succeed(
      new CohortTable.Service {
        override def fetch(
            filter: CohortTableFilter
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          assertEquals(filter, EstimationComplete)
          IO.succeed(
            ZStream(
              CohortItem(
                subscriptionName = expectedSubscriptionName,
                expectedStartDate = Some(expectedStartDate),
                currency = Some(expectedCurrency),
                oldPrice = Some(expectedOldPrice),
                estimatedNewPrice = Some(expectedEstimatedNewPrice)
              )
            )
          )
        }

        override def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] = ???
        override def update(result: AmendmentResult): ZIO[Any, CohortUpdateFailure, Unit] = ???

        override def update(
            subscriptionName: String,
            result: SalesforcePriceRiseCreationDetails
        ): ZIO[Any, CohortUpdateFailure, Unit] = {
          updatedResultsWrittenToCohortTable.addOne(result)
          IO.succeed(())
        }

        override def put(cohortItem: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = ???

        override def updateToCancelled(item: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = ???
      }
    )

    val createdPriceRises = ArrayBuffer[SalesforcePriceRise]()

    val stubSalesforceClient = ZLayer.succeed(
      new SalesforceClient.Service {
        override def getSubscriptionByName(
            subscriptionName: String
        ): IO[SalesforceClientFailure, SalesforceSubscription] = {
          IO.effect(
              SalesforceSubscription(s"SubscritionId-$subscriptionName", subscriptionName, s"Buyer-$subscriptionName")
            )
            .orElseFail(SalesforceClientFailure(""))
        }

        override def createPriceRise(
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResponse] = {
          createdPriceRises.addOne(priceRise)
          ZIO.succeed(SalesforcePriceRiseCreationResponse(s"${priceRise.SF_Subscription__c}-price-rise-id"))
        }
      }
    )

    assertEquals(
      default.unsafeRunSync(
        SalesforcePriceRiseCreationHandler.main
          .provideLayer(
            stubLogging ++ stubConfiguration ++ stubCohortTable ++ stubSalesforceClient ++ stubClock
          )
      ),
      Success(())
    )

    assertEquals(createdPriceRises.size, 1)
    assertEquals(createdPriceRises(0).SF_Subscription__c, s"SubscritionId-$expectedSubscriptionName")
    assertEquals(createdPriceRises(0).Buyer__c, s"Buyer-$expectedSubscriptionName")
    assertEquals(createdPriceRises(0).Current_Price_Today__c, expectedOldPrice)
    assertEquals(createdPriceRises(0).Guardian_Weekly_New_Price__c, expectedEstimatedNewPrice)
    assertEquals(createdPriceRises(0).Price_Rise_Date__c, expectedStartDate)

    assertEquals(updatedResultsWrittenToCohortTable.size, 1)
    assertEquals(
      updatedResultsWrittenToCohortTable(0).id,
      s"SubscritionId-$expectedSubscriptionName-price-rise-id"
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(0).whenSfShowEstimate,
      expectedCurrentTime
    )
  }
}
