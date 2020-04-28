package pricemigrationengine.services

import pricemigrationengine.model._
import zio.{ZIO, ZLayer}

object CohortTableTest {
  val impl: ZLayer[Any, Throwable, CohortTable] = ZLayer.succeed(
    new CohortTable.Service {

      def fetch(filter: CohortTableFilter, batchSize: Int): ZIO[Any, CohortFetchFailure, Set[CohortItem]] =
        ZIO.succeed(Set(CohortItem("A-S123"), CohortItem("A-S234"), CohortItem("A-S345")))

      def update(result: ResultOfEstimation): ZIO[Any, CohortUpdateFailure, Unit] = ZIO.succeed(())
    }
  )
}
