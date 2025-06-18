package pricemigrationengine.handlers

import pricemigrationengine.model.{
  CohortSpec,
  Failure,
  GuardianWeekly2025,
  HandlerOutput,
  MigrationType,
  Newspaper2025P1
}
import pricemigrationengine.services._
import zio.ZIO

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

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] =
    MigrationType(input) match {
      case Newspaper2025P1 => ZIO.succeed(HandlerOutput(isComplete = true))
      case _ => {
        main(input).provideSome[Logging](
          EnvConfig.stage.layer,
          DynamoDBClientLive.impl,
          CohortTableDdlLive.impl
        )
      }
    }
}
