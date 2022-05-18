package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.{CohortSpec, ConfigFailure}
import pricemigrationengine.services._
import zio.{Runtime, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

/** Executes price migration for active cohorts.
  */
object MigrationHandler extends ZIOAppDefault with RequestHandler[Unit, Unit] {

  private val migrateActiveCohorts =
    (for {
      today <- Time.today
      cohortSpecs <- CohortSpecTable.fetchAll
      activeSpecs <-
        ZIO
          .filter(cohortSpecs)(cohort => ZIO.succeed(CohortSpec.isActive(cohort)(today)))
          .tap(specs => Logging.info(s"Currently ${specs.size} active cohorts"))
      _ <- ZIO.foreachDiscard(activeSpecs)(CohortStateMachine.startExecution)
    } yield ()).tapError(e => Logging.error(s"Migration run failed: $e"))

  private val env: ZLayer[Logging, ConfigFailure, CohortSpecTable with CohortStateMachine with Logging] =
    (LiveLayer.cohortSpecTable and LiveLayer.cohortStateMachine and LiveLayer.logging)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))

  override def run: ZIO[ZIOAppArgs, Any, Any] =
    migrateActiveCohorts
      .provideLayer(ConsoleLogging.impl("MigrationHandler") to env)
      .exitCode

  override def handleRequest(unused: Unit, context: Context): Unit =
    Runtime.default.unsafeRun(
      migrateActiveCohorts
        .provideLayer(LambdaLogging.impl(context, "MigrationHandler") to env)
    )
}
