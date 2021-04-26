package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.{CohortSpec, ConfigurationFailure}
import pricemigrationengine.services._
import zio.{ExitCode, Runtime, ZEnv, ZIO, ZLayer}

/** Executes price migration for active cohorts.
  */
object MigrationHandler extends zio.App with RequestHandler[Unit, Unit] {

  private val migrateActiveCohorts =
    (for {
      today <- Time.today
      cohortSpecs <- CohortSpecTable.fetchAll
      activeSpecs <-
        ZIO
          .filter(cohortSpecs)(cohort => ZIO.succeed(CohortSpec.isActive(cohort)(today)))
          .tap(specs => Logging.info(s"Currently ${specs.size} active cohorts"))
      _ <- ZIO.foreach_(activeSpecs)(CohortStateMachine.startExecution)
    } yield ()).tapError(e => Logging.error(s"Migration run failed: $e"))

  private val env: ZLayer[Logging, ConfigurationFailure, CohortSpecTable with CohortStateMachine with Logging] =
    (LiveLayer.cohortSpecTable and LiveLayer.cohortStateMachine and LiveLayer.logging)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    migrateActiveCohorts
      .provideCustomLayer(ConsoleLogging.impl to env)
      .exitCode

  def handleRequest(unused: Unit, context: Context): Unit =
    Runtime.default.unsafeRun(
      migrateActiveCohorts
        .provideCustomLayer(LambdaLogging.impl(context) to env)
    )
}
