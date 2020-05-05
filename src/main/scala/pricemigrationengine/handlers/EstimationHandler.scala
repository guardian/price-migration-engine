package pricemigrationengine.handlers

import java.time.LocalDate

import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model.{CohortItem, EstimationFailed, Failure, ResultOfEstimation}
import pricemigrationengine.services.{CohortTable, CohortTableTest, Zuora, ZuoraTest}
import zio.console.Console
import zio.{App, ZEnv, ZIO, console}

object EstimationHandler extends App {

  // TODO: configuration
  val batchSize = 100
  val earliestStartDate = LocalDate.now

  val main: ZIO[Console with CohortTable with Zuora, Failure, Unit] =
    for {
      cohortItems <- CohortTable.fetch(ReadyForEstimation, batchSize)
      results <- ZIO.foreach(cohortItems)(item => estimation(item, earliestStartDate))
      _ <- ZIO.foreach(results)(CohortTable.update)
    } yield ()

  def estimation(item: CohortItem, earliestStartDate: LocalDate): ZIO[Zuora, Nothing, ResultOfEstimation] = {
    val result = for {
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      invoices <- Zuora.fetchInvoicePreview(subscription.accountId)
    } yield ResultOfEstimation(subscription, invoices, earliestStartDate)
    result.catchAll(e => ZIO.succeed(EstimationFailed(item.subscriptionName, e)))
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
