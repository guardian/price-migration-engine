package pricemigrationengine.services

import pricemigrationengine.model._
import zio.console.Console
import zio.stream.ZStream
import zio.{UIO, ZIO, ZLayer}

object CohortTableTest {
  val impl: ZLayer[Console, Nothing, CohortTable] = ZLayer.fromService(
    console =>
      new CohortTable.Service {
        def fetch(
            filter: CohortTableFilter
        ): UIO[ZStream[Any, CohortFetchFailure, CohortItem]] = {
          val items: ZStream[Any, CohortFetchFailure, CohortItem] =
            ZStream(CohortItem("A-S123"), CohortItem("A-S234"), CohortItem("A-S345"))
              .mapM(item => ZIO.effect(item).mapError(_ => CohortFetchFailure("")))
          for {
            _ <- console.putStrLn(s"Fetched from cohort table: $items")
          } yield items
        }

        def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] =
          for {
            _ <- console.putStrLn(s"Updating cohort table with result: $result")
          } yield ()

        def update(subscriptionName: String, result: SalesforcePriceRiseCreationDetails): ZIO[Any, CohortUpdateFailure, Unit] = ???
      }
  )
}
