package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model.{CohortItem, EstimationFailed, Failure, ResultOfEstimation}
import pricemigrationengine.services.{CohortTable, CohortTableTest, Zuora, ZuoraTest}
import zio.console.Console
import zio.{App, ZEnv, ZIO, console}

object EstimationHandler extends App {

  val batchSize = 100

  val main: ZIO[Console with CohortTable with Zuora, Failure, Unit] =
    for {
      cohortItems <- CohortTable.fetch(ReadyForEstimation, batchSize)
      results <- ZIO.foreach(cohortItems)(estimation)
      _ <- ZIO.foreach(results)(CohortTable.update)
    } yield ()

  def estimation(item: CohortItem): ZIO[Zuora, Nothing, ResultOfEstimation] =
    Zuora
      .fetchSubscription(item.subscriptionName)
      .map(sub => ResultOfEstimation.fromSubscription(sub))
      .catchAll(e => ZIO.succeed(EstimationFailed(item.subscriptionName, e)))

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideCustomLayer(
        CohortTableTest.impl ++ ZuoraTest.impl
      )
      .foldM(
        e => console.putStrLn(s"Failed: $e") *> ZIO.succeed(1),
        _ => ZIO.succeed(0)
      )
}
