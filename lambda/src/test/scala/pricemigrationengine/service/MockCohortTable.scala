package pricemigrationengine.service

import pricemigrationengine.model.{CohortFetchFailure, CohortItem, CohortTableFilter, CohortUpdateFailure, Failure}
import pricemigrationengine.services.CohortTable
import zio.mock.{Mock, Proxy}
import zio.stream.ZStream
import zio.{IO, URLayer, ZIO, ZLayer}

import java.time.LocalDate

object MockCohortTable extends Mock[CohortTable] {

  object Fetch extends Stream[(CohortTableFilter, Option[LocalDate]), CohortFetchFailure, CohortItem]
  object FetchAll extends Stream[Unit, CohortFetchFailure, CohortItem]
  object Create extends Effect[CohortItem, Failure, Unit]
  object Update extends Effect[CohortItem, CohortUpdateFailure, Unit]

  val compose: URLayer[Proxy, CohortTable] = ZLayer.fromZIO(
    ZIO.serviceWithZIO[Proxy](proxy =>
      ZIO
        .runtime[Any]
        .map(runtime =>
          new CohortTable {

            override def fetch(filter: CohortTableFilter, beforeDateInclusive: Option[LocalDate])
                : ZStream[Any, CohortFetchFailure, CohortItem] =
              runtime.unsafeRun(proxy(Fetch, filter, beforeDateInclusive))

            override def fetchAll(): ZStream[Any, CohortFetchFailure, CohortItem] =
              runtime.unsafeRun(proxy(FetchAll, ()))

            override def create(cohortItem: CohortItem): IO[Failure, Unit] =
              proxy(Create, cohortItem)

            override def update(result: CohortItem): IO[CohortUpdateFailure, Unit] =
              proxy(Update, result)
          }
        )
    )
  )
}
