package pricemigrationengine.services

import pricemigrationengine.model.{CohortItem, Failure}
import zio.{UIO, ZIO}

trait Logging {
  def info(s: String): UIO[Unit]
  def error(s: String): UIO[Unit]

  def logSuccess[A](cohortItem: CohortItem)(result: A): UIO[Unit] =
    info(s"Subscription ${cohortItem.subscriptionName} succeeded: $result")

  def logFailure(cohortItem: CohortItem)(failure: Failure): UIO[Unit] =
    error(s"Subscription ${cohortItem.subscriptionName} failed: $failure")
}

object Logging {

  def info(s: String): ZIO[Logging, Nothing, Unit] =
    ZIO.environmentWithZIO(_.get.info(s))

  def error(s: String): ZIO[Logging, Nothing, Unit] =
    ZIO.environmentWithZIO(_.get.error(s))

  def logSuccess[A](cohortItem: CohortItem)(result: A): ZIO[Logging, Nothing, Unit] =
    ZIO.environmentWithZIO(_.get.logSuccess(cohortItem)(result))

  def logFailure(cohortItem: CohortItem)(failure: Failure): ZIO[Logging, Nothing, Unit] =
    ZIO.environmentWithZIO(_.get.logFailure(cohortItem)(failure))
}
