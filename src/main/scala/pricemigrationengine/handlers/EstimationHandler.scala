package pricemigrationengine.handlers

import java.time.LocalDate

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.console.Console
import zio.{App, Runtime, ZEnv, ZIO, ZLayer}

object EstimationHandler extends App with RequestHandler[Unit, Unit] {

  // TODO: configuration
  val batchSize = 100
  val earliestStartDate = LocalDate.now

  def estimation(item: CohortItem, earliestStartDate: LocalDate): ZIO[Zuora, Failure, EstimationResult] = {
    val result = for {
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId)
    } yield EstimationResult(subscription, invoicePreview, earliestStartDate)
    result.absolve
  }

  val main: ZIO[Logging with CohortTable with Zuora, Failure, Unit] =
    for {
      cohortItems <- CohortTable
        .fetch(ReadyForEstimation, batchSize)
        .tapBoth(
          e => Logging.error(s"Failed to fetch from Cohort table: $e"),
          items => Logging.info(s"Fetched ${items.size} subs from Cohort table: $items")
        )
      results <- ZIO.foreach(cohortItems)(
        item =>
          estimation(item, earliestStartDate).tapBoth(
            e => Logging.error(s"Failed to estimate amendment data: $e"),
            result => Logging.info(s"Estimated result: $result")
        )
      )
      _ <- ZIO.foreach(results)(
        result =>
          CohortTable
            .update(result)
            .tapBoth(
              e => Logging.error(s"Failed to update Cohort table: $e"),
              _ => Logging.info(s"Wrote $result to Cohort table")
          )
      )
    } yield ()

  private val env = CohortTableTest.impl ++ ZuoraTest.impl
  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideCustomLayer(
        env ++ ConsoleLogging.impl
      )
      .fold(_ => 1, _ => 0)

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideCustomLayer(
        env ++ LambdaLogging.impl(context)
      )
    )
}
