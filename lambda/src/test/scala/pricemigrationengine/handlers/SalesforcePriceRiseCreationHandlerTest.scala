package pricemigrationengine.handlers

import java.time.LocalDate

import pricemigrationengine.model.CohortTableFilter.EstimationComplete
import pricemigrationengine.model.{CohortFetchFailure, CohortItem, CohortTableFilter, CohortUpdateFailure, ConfigurationFailure, EstimationHandlerConfig, EstimationResult, SalesforceClientFailure, SalesforcePriceRise, SalesforcePriceRiseCreationResult, SalesforceSubscription}
import pricemigrationengine.services.{CohortTable, ConsoleLogging, EstimationHandlerConfiguration, SalesforceClient}
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer, console}

import scala.collection.mutable.ArrayBuffer

class SalesforcePriceRiseCreationHandlerTest extends munit.FunSuite {
  test("SalesforcePriceRiseCreateHandler should get estimated prices from ") {
    val stubConfiguration = ZLayer.succeed(
      new EstimationHandlerConfiguration.Service {
        override val config: IO[ConfigurationFailure, EstimationHandlerConfig] =
          IO.succeed(EstimationHandlerConfig(LocalDate.now))
      }
    )

    val stubLogging = console.Console.live >>> ConsoleLogging.impl

    val expectedSubscriptionName = "Sub-0001"
    val expectedStartDate = LocalDate.of(2020, 1, 1)
    val expectedCurrency = "GBP"
    val expectedOldPrice = BigDecimal(11.11)
    val expectedEstimatedNewPrice = BigDecimal(22.22)

    val updatedResultsWrittenToCohortTable = ArrayBuffer[SalesforcePriceRiseCreationResult]()

    val stubCohortTable = ZLayer.succeed(
      new CohortTable.Service {
        override def fetch(
          filter: CohortTableFilter
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          assertEquals(filter, EstimationComplete)
          IO.succeed(ZStream(
            CohortItem(
              subscriptionName = expectedSubscriptionName,
              expectedStartDate = Some(expectedStartDate),
              currency = Some(expectedCurrency),
              oldPrice = Some(expectedOldPrice),
              estimatedNewPrice = Some(expectedEstimatedNewPrice)
            ),
          ))
        }

        override def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] = ???

        override def update(subscriptionName: String, result: SalesforcePriceRiseCreationResult): ZIO[Any, CohortUpdateFailure, Unit] = {
          updatedResultsWrittenToCohortTable.addOne(result)
          IO.succeed(())
        }
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
          ).mapError(_ => SalesforceClientFailure(""))
        }

        override def createPriceRise(priceRise: SalesforcePriceRise): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResult] = {
          createdPriceRises.addOne(priceRise)
          ZIO.succeed(SalesforcePriceRiseCreationResult(s"${priceRise.SF_Subscription__c}-price-rise-id"))
        }
      }
    )

    assertEquals(
      default.unsafeRunSync(
        SalesforcePriceRiseCreationHandler
          .main
          .provideLayer(
            stubLogging ++ stubConfiguration ++ stubCohortTable ++ stubSalesforceClient
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
  }
}
