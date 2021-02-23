package pricemigrationengine.services

import com.amazonaws.services.dynamodbv2.model.CreateTableResult
import pricemigrationengine.model.{CohortSpec, CohortTableCreateFailure}
import zio.{IO, ZIO}

object CohortTableDdl {

  /** Service to run DDL statements on CohortTables.
    * Creation and dropping, etc.
    */
  trait Service {

    /** Create a table for the given CohortSpec if it doesn't already exist.
      * Otherwise do nothing.
      */
    def createTable(cohortSpec: CohortSpec): IO[CohortTableCreateFailure, Option[CreateTableResult]]
  }

  def createTable(cohortSpec: CohortSpec): ZIO[CohortTableDdl, CohortTableCreateFailure, Option[CreateTableResult]] =
    ZIO.accessM(_.get.createTable(cohortSpec))
}
