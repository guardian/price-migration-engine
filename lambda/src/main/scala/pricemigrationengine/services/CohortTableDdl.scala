package pricemigrationengine.services

import pricemigrationengine.model.{CohortSpec, CohortTableCreateFailure}
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse
import zio.{IO, ZIO}

object CohortTableDdl {

  /** Service to run DDL statements on CohortTables. Creation and dropping, etc.
    */
  trait Service {

    /** Create a table for the given CohortSpec if it doesn't already exist. Otherwise do nothing.
      */
    def createTable(cohortSpec: CohortSpec): IO[CohortTableCreateFailure, Option[CreateTableResponse]]
  }

  def createTable(cohortSpec: CohortSpec): ZIO[CohortTableDdl, CohortTableCreateFailure, Option[CreateTableResponse]] =
    ZIO.accessM(_.get.createTable(cohortSpec))
}
