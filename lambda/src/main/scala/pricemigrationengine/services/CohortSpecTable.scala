package pricemigrationengine.services

import pricemigrationengine.model.{CohortSpec, CohortSpecFetchFailure, CohortSpecUpdateFailure}
import zio.stream.ZStream
import zio.{IO, ZIO}

/**
  * For accessing the specifications of each cohort.
  */
object CohortSpecTable {

  trait Service {
    val fetchAll: IO[CohortSpecFetchFailure, ZStream[Any, CohortSpecFetchFailure, CohortSpec]]
    def update(spec: CohortSpec): ZIO[Any, CohortSpecUpdateFailure, Unit]
  }

  val fetchAll: ZIO[CohortSpecTable, CohortSpecFetchFailure, ZStream[Any, CohortSpecFetchFailure, CohortSpec]] =
    ZIO.accessM(_.get.fetchAll)

  def update(spec: CohortSpec): ZIO[CohortSpecTable, CohortSpecUpdateFailure, Unit] =
    ZIO.accessM(_.get.update(spec))
}
