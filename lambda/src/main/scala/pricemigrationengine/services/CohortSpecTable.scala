package pricemigrationengine.services

import pricemigrationengine.model.{CohortSpec, CohortSpecUpdateFailure, Failure}
import zio.{IO, ZIO}

/** For accessing the specifications of each cohort.
  */
object CohortSpecTable {

  trait Service {
    val fetchAll: IO[Failure, Set[CohortSpec]]
    def update(spec: CohortSpec): ZIO[Any, CohortSpecUpdateFailure, Unit]
  }

  val fetchAll: ZIO[CohortSpecTable, Failure, Set[CohortSpec]] =
    ZIO.accessM(_.get.fetchAll)

  def update(spec: CohortSpec): ZIO[CohortSpecTable, CohortSpecUpdateFailure, Unit] =
    ZIO.accessM(_.get.update(spec))
}
