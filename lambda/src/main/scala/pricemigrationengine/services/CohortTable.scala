package pricemigrationengine.services

import pricemigrationengine.model._
import zio.stream.ZStream
import zio.{IO, ZIO}

case class CohortTableKey(subscriptionNumber: String)

object CohortTable {
  trait Service {
    def fetch(
        filter: CohortTableFilter
    ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]]

    def put(cohortItem: CohortItem): ZIO[Any, CohortUpdateFailure, Unit]
    def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit]
    def update(result: EstimationResult): ZIO[Any, CohortUpdateFailure, Unit]
    def update(subscriptionName: String, result: SalesforcePriceRiseCreationDetails): ZIO[Any, CohortUpdateFailure, Unit]
    def update(result: AmendmentResult): ZIO[Any, CohortUpdateFailure, Unit]
  }

  def fetch(
      filter: CohortTableFilter
  ): ZIO[CohortTable, CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] =
    ZIO.accessM(_.get.fetch(filter))

  def put(subscription: CohortItem): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.accessM(_.get.put(subscription))

  def update(result: EstimationResult): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.accessM(_.get.update(result))

  def update(
    subscriptionName: String,
    result: SalesforcePriceRiseCreationDetails
  ): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.accessM(_.get.update(subscriptionName, result))

  def update(result: AmendmentResult): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.accessM(_.get.update(result))

  def update(result: CohortItem): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.accessM(_.get.update(result))
}
