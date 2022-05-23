package pricemigrationengine.service

import pricemigrationengine.model.{CohortFetchFailure, CohortItem, CohortTableFilter, CohortUpdateFailure, Failure}
import pricemigrationengine.services.CohortTable
import zio.mock.{Mock, Proxy}
import zio.stream.ZStream
import zio.{IO, URLayer, ZIO, ZLayer}

import java.time.LocalDate

object MockCohortTable extends Mock[CohortTable] {

  object Fetch
      extends Effect[
        (CohortTableFilter, Option[LocalDate]),
        CohortFetchFailure,
        ZStream[Any, CohortFetchFailure, CohortItem]
      ]
  object FetchAll extends Effect[Unit, CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]]
  object Create extends Effect[CohortItem, Failure, Unit]
  object Update extends Effect[CohortItem, CohortUpdateFailure, Unit]

  val compose: URLayer[Proxy, CohortTable] = ZLayer.fromZIO(ZIO.service[Proxy].map { proxy =>
    new CohortTable {

      override def fetch(filter: CohortTableFilter, beforeDateInclusive: Option[LocalDate])
          : IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] =
        proxy(Fetch, filter, beforeDateInclusive)

      override def fetchAll(): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] =
        proxy(FetchAll, ())

      override def create(cohortItem: CohortItem): IO[Failure, Unit] =
        proxy(Create, cohortItem)

      override def update(result: CohortItem): IO[CohortUpdateFailure, Unit] =
        proxy(Update, result)
    }
  })
}
