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
  ): ZStream[Any, CohortFetchFailure, CohortItem]

  def fetchAll(): ZStream[Any, CohortFetchFailure, CohortItem]

  def create(cohortItem: CohortItem): IO[Failure, Unit]

  def update(cohortItem: CohortItem): IO[CohortUpdateFailure, Unit]
}

object CohortTable {

  def fetch(
      filter: CohortTableFilter,
      beforeDateInclusive: Option[LocalDate]
  ): ZStream[CohortTable, CohortFetchFailure, CohortItem] =
    ZStream.serviceWithStream(_.fetch(filter, beforeDateInclusive))

  def fetchAll(): ZStream[CohortTable, CohortFetchFailure, CohortItem] =
    ZStream.serviceWithStream(_.fetchAll())

  def create(subscription: CohortItem): ZIO[CohortTable, Failure, Unit] =
    ZIO.environmentWithZIO(_.get.create(subscription))

  def update(cohortItem: CohortItem): ZIO[CohortTable, CohortUpdateFailure, Unit] =
    ZIO.environmentWithZIO(_.get.update(cohortItem))
}
