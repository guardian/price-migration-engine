package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortSpec
import pricemigrationengine.services.{CohortSpecTable, CohortStateMachine}
import zio.{ExitCode, Runtime, ULayer, ZEnv, ZIO}

/**
  * Executes price migration for active cohorts.
  */
class MigrationHandler extends zio.App with RequestHandler[Unit, Unit] {

  private val migrateActiveCohorts =
    for {
      cohortSpecStream <- CohortSpecTable.fetchAll
      _ <- cohortSpecStream
        .filterM(cohort => Time.today.map(CohortSpec.isActive(cohort)))
        .foreach(cohort => CohortStateMachine.startExecution(cohort))
    } yield ()

  private val runtime = Runtime.default

  private val env: ULayer[CohortSpecTable with CohortStateMachine] = ???

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    migrateActiveCohorts.provideCustomLayer(env).exitCode

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(migrateActiveCohorts.provideCustomLayer(env))
}
