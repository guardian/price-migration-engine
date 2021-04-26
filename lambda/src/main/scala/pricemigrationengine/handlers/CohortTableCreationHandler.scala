package pricemigrationengine.handlers

import pricemigrationengine.model.{CohortSpec, ConfigurationFailure, Failure, HandlerOutput}
import pricemigrationengine.services.{CohortTableDdl, Logging}
import zio.{ZEnv, ZIO, ZLayer}

/** Creates a new CohortTable if it doesn't already exist.
  */
object CohortTableCreationHandler extends CohortHandler {

  def main(cohortSpec: CohortSpec): ZIO[CohortTableDdl with Logging, Failure, HandlerOutput] =
    CohortTableDdl
      .createTable(cohortSpec)
      .tapBoth(
        e => Logging.error(s"Failed to create table for $cohortSpec: $e"),
        {
          case None         => Logging.info(s"No action. Table for $cohortSpec already exists.")
          case Some(result) => Logging.info(s"Created table: $result")
        }
      )
      .as(HandlerOutput(isComplete = true))

  private val env: ZLayer[Logging, ConfigurationFailure, CohortTableDdl with Logging] =
    (LiveLayer.cohortTableDdl and LiveLayer.logging)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))

  def handle(input: CohortSpec): ZIO[ZEnv with Logging, Failure, HandlerOutput] =
    main(input).provideSomeLayer[ZEnv with Logging](env)
}
