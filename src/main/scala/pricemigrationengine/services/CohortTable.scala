package pricemigrationengine.services

import pricemigrationengine.model._
import zio.ZIO

object CohortTable {

  trait Service {
    def fetch(filter: CohortTableFilter, batchSize: Int): ZIO[Any, CohortFetchFailure, Set[CohortItem]]

    def update(result: ResultOfEstimation): ZIO[Any, CohortUpdateFailure, Unit]
  }

  def fetch(filter: CohortTableFilter, batchSize: Int): ZIO[CohortTable, CohortFetchFailure, Set[CohortItem]] =
    ZIO.accessM(_.get.fetch(filter, batchSize))

  def update(result: ResultOfEstimation): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.accessM(_.get.update(result))
}
