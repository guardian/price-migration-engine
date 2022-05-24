package pricemigrationengine.services

import pricemigrationengine.model.{CohortSpec, CohortSpecUpdateFailure, Failure}
import zio.{IO, ZIO}

/** For accessing the specifications of each cohort.
  */
trait CohortSpecTable {
  val fetchAll: IO[Failure, Set[CohortSpec]]
  def update(spec: CohortSpec): ZIO[Any, CohortSpecUpdateFailure, Unit]
}

object CohortSpecTable {

  val fetchAll: ZIO[CohortSpecTable, Failure, Set[CohortSpec]] =
    ZIO.environmentWithZIO(_.get.fetchAll)

  def update(spec: CohortSpec): ZIO[CohortSpecTable, CohortSpecUpdateFailure, Unit] =
    ZIO.environmentWithZIO(_.get.update(spec))
}
