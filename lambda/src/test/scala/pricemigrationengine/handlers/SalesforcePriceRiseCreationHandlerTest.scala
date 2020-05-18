package pricemigrationengine.handlers

import java.time.LocalDate

import pricemigrationengine.model.CohortTableFilter.EstimationComplete
import pricemigrationengine.model.{CohortFetchFailure, CohortItem, CohortTableFilter, CohortUpdateFailure, Config, ConfigurationFailure, DynamoDBConfig, EstimationResult, ZuoraConfig}
import pricemigrationengine.services.{CohortTable, Configuration, ConsoleLogging}
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.ZStream
import zio.{IO, ZIO, ZLayer, console}

class SalesforcePriceRiseCreationHandlerTest extends munit.FunSuite {
  test("SalesforcePriceRiseCreateHandler should get estimated prices from ") {
    val expectedBatchSize = 101
    val stubConfiguration = ZLayer.succeed(
      new Configuration.Service {
        override val config: IO[ConfigurationFailure, Config] =
          IO.succeed(Config(DynamoDBConfig(None), "DEV", LocalDate.now, batchSize = expectedBatchSize))
      }
    )

    val stubLogging = console.Console.live >>> ConsoleLogging.impl

    val stubCohortTable = ZLayer.succeed(
      new CohortTable.Service {
        override def fetch(
          filter: CohortTableFilter,
          batchSize: Int
        ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] = {
          assertEquals(batchSize, expectedBatchSize)
          assertEquals(filter, EstimationComplete)
          IO.succeed(ZStream.empty)
        }

        override def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] = ???

        override def update(result: SalesforcePriceRiseCreationResult): ZIO[Any, CohortUpdateFailure, Unit] =
          IO.succeed(())
      }
    )

    assertEquals(
      default.unsafeRunSync(
        SalesforcePriceRiseCreationHandler
          .main
          .provideLayer(
            stubLogging ++ stubConfiguration ++ stubCohortTable
          )
      ),
      Success(())
    )
  }
}
