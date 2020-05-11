package pricemigrationengine.services

import pricemigrationengine.model._
import zio.console.Console
import zio.{ZIO, ZLayer}

object CohortTableTest {
  val impl: ZLayer[Console, Nothing, CohortTable] = ZLayer.fromService(
    console =>
      new CohortTable.Service {

        def fetch(filter: CohortTableFilter, batchSize: Int): ZIO[Any, CohortFetchFailure, Set[CohortItem]] = {
          val items = Set(CohortItem("A-S123"), CohortItem("A-S234"), CohortItem("A-S345"))
          for {
            _ <- console.putStrLn(s"Fetched from cohort table: $items")
          } yield items
        }

        def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit] =
          for {
            _ <- console.putStrLn(s"Updating cohort table with result: $result")
          } yield ()
    }
  )
}
