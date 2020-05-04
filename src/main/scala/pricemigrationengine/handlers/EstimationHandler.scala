package pricemigrationengine.handlers

import java.time.LocalDate

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.services.{CohortTable, CohortTableTest, Zuora, ZuoraTest}
import zio.clock.Clock
import zio.console.Console
import zio._

object EstimationHandler extends App {

  // TODO: configuration
  val batchSize = 100
  val earliestStartDate = LocalDate.now

  val main: ZIO[Console with Clock with CohortTable with Zuora, Failure, Unit] =
    for {
      cohortItems <- CohortTable.fetch(ReadyForEstimation, batchSize)
      results <- ZIO.foreach(cohortItems)(item => estimation(item, earliestStartDate))
      _ <- ZIO.foreach(results)(CohortTable.update)
    } yield ()

  def estimation(item: CohortItem, earliestStartDate: LocalDate): ZIO[Clock with Zuora, Failure, EstimationResult] = {
    val result = for {
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      account <- Zuora.fetchAccount(subscription.accountNumber)
      currentDate <- clock.currentDateTime.bimap(e => AmendmentDataFailure(e.getMessage), _.toLocalDate)
    } yield EstimationResult(subscription, account, earliestStartDate, currentDate)
    result.absolve
  }

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
