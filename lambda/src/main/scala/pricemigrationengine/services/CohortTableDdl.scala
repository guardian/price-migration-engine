package pricemigrationengine.services

import com.amazonaws.services.dynamodbv2.model.CreateTableResult
import pricemigrationengine.model.CohortTableCreateFailure
import zio.{IO, ZIO}

object CohortTableDdl {

  /**
    * Service to run DDL statements on CohortTables.
    * Creation and dropping, etc.
    */
  trait Service {

    /**
      * Create a table if it doesn't already exist.
      * Otherwise do nothing.
      */
    def createTable(tableName: String): IO[CohortTableCreateFailure, Option[CreateTableResult]]
  }

  def createTable(tableName: String): ZIO[CohortTableDdl, CohortTableCreateFailure, Option[CreateTableResult]] =
    ZIO.accessM(_.get.createTable(tableName))
}
