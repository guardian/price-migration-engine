package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortSpec
import pricemigrationengine.services.{CohortSpecTable, CohortStateMachine}
import zio.{ExitCode, Runtime, ULayer, ZEnv, ZIO}

/**
  * Executes price migration for active cohorts.
  */
object MigrationHandler extends zio.App with RequestHandler[Unit, Unit] {

  private val migrateActiveCohorts =
    for {
      cohortSpecs <- CohortSpecTable.fetchAll
      activeSpecs <- ZIO.filter(cohortSpecs)(cohort => Time.today.map(CohortSpec.isActive(cohort)))
      _ <- ZIO.foreach(activeSpecs)(CohortStateMachine.startExecution)
    } yield ()

  private val runtime = Runtime.default

  private val env: ULayer[CohortSpecTable with CohortStateMachine] = ???

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    migrateActiveCohorts.provideCustomLayer(env).exitCode

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(migrateActiveCohorts.provideCustomLayer(env))
}
