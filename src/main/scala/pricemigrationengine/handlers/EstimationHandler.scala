package pricemigrationengine.handlers

import java.time.LocalDate

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.dynamodb.{DynamoDBClient, DynamoDBZIO}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.services.{CohortTable, Zuora, ZuoraTest, _}
import zio.clock.Clock
import zio.console.Console
import zio.{App, Runtime, ZEnv, ZIO}

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
        .tapError(
          e => Logging.error(s"Failed to fetch from Cohort table: $e")
        )
      results = cohortItems.mapM(
        item =>
          estimation(item, earliestStartDate).tapBoth(
            e => Logging.error(s"Failed to estimate amendment data: $e"),
            result => Logging.info(s"Estimated result: $result")
        )
      )
      _ <- results.foreach(
        result =>
          CohortTable
            .update(result)
            .tapBoth(
              e => Logging.error(s"Failed to update Cohort table: $e"),
              _ => Logging.info(s"Wrote $result to Cohort table")
          )
      )
    } yield ()

  private val env =
    DynamoDBClient.dynamoDB ++ Console.live >>>
    DynamoDBZIO.impl ++ Console.live >>>
    CohortTable.impl ++ ZuoraTest.impl

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
