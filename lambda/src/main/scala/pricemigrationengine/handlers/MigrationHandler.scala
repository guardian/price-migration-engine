package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.Runner
import pricemigrationengine.services._
import zio.{Clock, Runtime, ZIO, ZIOAppArgs, ZIOAppDefault}

/** Executes price migration for active cohorts.
  */
object MigrationHandler extends ZIOAppDefault with RequestHandler[Unit, Unit] {

  private val migrateActiveCohorts =
    (for {
      cohortSpecs <- CohortSpecTable.fetchAll
      activeSpecs <-
        ZIO
          .filter(cohortSpecs)(cohort => ZIO.succeed(true)) // TODO: simplify this
          .tap(specs => Logging.info(s"Currently ${specs.size} active cohorts"))
      _ <- ZIO.foreachDiscard(activeSpecs)(CohortStateMachine.startExecution)
    } yield ()).tapError(e => Logging.error(s"Migration run failed: $e"))

  override def run: ZIO[ZIOAppArgs, Any, Any] =
    migrateActiveCohorts
      .provide(
        ConsoleLogging.impl("MigrationHandler"),
        EnvConfig.cohortStateMachine.layer,
        EnvConfig.stage.layer,
        DynamoDBClientLive.impl,
        CohortSpecTableLive.impl,
        CohortStateMachineLive.impl
      )

  override def handleRequest(unused: Unit, context: Context): Unit =
    Runner.unsafeRun(Runtime.default)(
      migrateActiveCohorts
        .provide(
          LambdaLogging.impl(context, "MigrationHandler"),
          EnvConfig.cohortStateMachine.layer,
          EnvConfig.stage.layer,
          DynamoDBClientLive.impl,
          CohortSpecTableLive.impl,
          CohortStateMachineLive.impl
        )
    )
}
