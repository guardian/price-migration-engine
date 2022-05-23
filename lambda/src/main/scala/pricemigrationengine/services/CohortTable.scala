package pricemigrationengine.services

import java.time.LocalDate

import pricemigrationengine.model._
import zio.stream.ZStream
import zio.{IO, ZIO}

case class CohortTableKey(subscriptionNumber: String)

trait CohortTable {
  def fetch(
      filter: CohortTableFilter,
      beforeDateInclusive: Option[LocalDate]
  ): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]]

  def fetchAll(): IO[CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]]

  def create(cohortItem: CohortItem): IO[Failure, Unit]

  def update(result: CohortItem): IO[CohortUpdateFailure, Unit]
}

object CohortTable {

  def fetch(
      filter: CohortTableFilter,
      beforeDateInclusive: Option[LocalDate]
  ): ZIO[CohortTable, CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] =
    ZIO.environmentWithZIO(_.get.fetch(filter, beforeDateInclusive))

  def fetchAll(): ZIO[CohortTable, CohortFetchFailure, ZStream[Any, CohortFetchFailure, CohortItem]] =
    ZIO.environmentWithZIO(_.get.fetchAll())

  def create(subscription: CohortItem): ZIO[CohortTable, Failure, Unit] =
    ZIO.environmentWithZIO(_.get.create(subscription))

  def update(result: CohortItem): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.environmentWithZIO(_.get.update(result))
}
