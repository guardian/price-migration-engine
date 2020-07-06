package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortSpec
import pricemigrationengine.services._
import zio.console.Console
import zio.{ExitCode, Runtime, ZEnv, ZIO, ZLayer}

/**
  * Executes price migration for active cohorts.
  */
object MigrationHandler extends zio.App with RequestHandler[Unit, Unit] {

  private val migrateActiveCohorts =
    (for {
      today <- Time.today
      cohortSpecs <- CohortSpecTable.fetchAll
      activeSpecs <- ZIO
        .filter(cohortSpecs)(cohort => ZIO.succeed(CohortSpec.isActive(cohort)(today)))
        .tap(specs => Logging.info(s"Currently ${specs.size} active cohorts"))
      _ <- ZIO.foreach(activeSpecs)(CohortStateMachine.startExecution(today))
    } yield ()).tapError(e => Logging.error(s"Migration run failed: $e"))

  private def env(loggingService: Logging.Service) =
    ZLayer.succeed(loggingService) and EnvConfiguration.dynamoDbImpl andTo
      DynamoDBClient.dynamoDB andTo
      EnvConfiguration.stageImp andTo
      EnvConfiguration.cohortStateMachineImpl andTo
      (CohortSpecTableLive.impl and CohortStateMachineLive.impl)
        .tapError(e => loggingService.error(s"Failed to create service environment: $e"))

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    migrateActiveCohorts
      .provideCustomLayer(env(ConsoleLogging.service(Console.Service.live)))
      .exitCode

  def handleRequest(unused: Unit, context: Context): Unit =
    Runtime.default.unsafeRun(
      migrateActiveCohorts
        .provideCustomLayer(env(LambdaLogging.service(context)))
    )
}
