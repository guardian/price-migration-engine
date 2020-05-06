package pricemigrationengine.handlers

import java.time.LocalDate

import pricemigrationengine.dynamodb.{DynamoDBClient, DynamoDBZIO}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.services.{CohortTable, UserRepo, Zuora, ZuoraTest}
import zio.clock.Clock
import zio.console.Console
import zio.{App, ZEnv, ZIO, ZLayer, console}

object EstimationHandler extends App {

  // TODO: configuration
  val batchSize = 100
  val earliestStartDate = LocalDate.now

  val main: ZIO[CohortTable with Clock with Zuora, Failure, Unit] =
    for {
      cohortItems <- CohortTable.fetch(ReadyForEstimation, batchSize)
      results = cohortItems.mapM(item => estimation(item, earliestStartDate))
      updateResult <- results.foreach(result => CohortTable.update(result))
    } yield updateResult

  def estimation(item: CohortItem, earliestStartDate: LocalDate): ZIO[Zuora, Failure, EstimationResult] = {
    val result = for {
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId)
    } yield EstimationResult(subscription, invoicePreview, earliestStartDate)
    result.absolve
  }

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    main
      .provideCustomLayer(
        DynamoDBClient.dynamoDB ++ Console.live >>>
        DynamoDBZIO.impl ++ Console.live >>>
        CohortTable.impl ++ ZuoraTest.impl
      )
      .foldM(
        e => console.putStrLn(s"Failed: $e") *> ZIO.succeed(1),
        _ => ZIO.succeed(0)
      )
  }
}
