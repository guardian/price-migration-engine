package pricemigrationengine.handlers

import java.time.LocalDate

import pricemigrationengine.model.CohortTableFilter.EstimationComplete
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer, console}

class SalesforcePriceRiseCreationHandlerTest extends munit.FunSuite {
  test("SalesforcePriceRiseCreateHandler should get estimated prices from ") {
    val stubConfiguration = ZLayer.succeed(
      new AmendmentConfiguration.Service {
        override val config: IO[ConfigurationFailure, AmendmentConfig] =
          IO.succeed(AmendmentConfig(LocalDate.now))
      }
    )

    val stubLogging = console.Console.live >>> ConsoleLogging.impl

    val stubCohortTable = ZLayer.succeed(
      new CohortTable.Service {
        override def fetch(
            filter: CohortTableFilter
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          assertEquals(filter, EstimationComplete)
          IO.succeed(ZStream(CohortItem("Sub-0001"), CohortItem("Sub-0002")))
        }

        override def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] = ???
        override def update(result: AmendmentResult): ZIO[Any, CohortUpdateFailure, Unit] = ???

        override def update(result: SalesforcePriceRiseCreationResult): ZIO[Any, CohortUpdateFailure, Unit] =
          IO.succeed(())
      }
    )

    val stubSalesforceClient = ZLayer.succeed(
      new SalesforceClient.Service {
        override def getSubscriptionByName(
            subscrptionName: String
        ): IO[SalesforceClientFailure, SalesforceSubscription] = {
          IO.effect(
              SalesforceSubscription(s"SubscritionId-$subscrptionName", subscrptionName, s"Buyer-$subscrptionName")
            )
            .mapError(_ => SalesforceClientFailure(""))
        }
      }
    )

    assertEquals(
      default.unsafeRunSync(
        SalesforcePriceRiseCreationHandler.main
          .provideLayer(
            stubLogging ++ stubConfiguration ++ stubCohortTable ++ stubSalesforceClient
          )
      ),
      Success(())
    )
  }
}
