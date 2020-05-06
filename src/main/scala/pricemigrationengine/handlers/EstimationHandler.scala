package pricemigrationengine.handlers

import java.time.LocalDate

import pricemigrationengine.dynamodb.{DynamoDBClient, DynamoDBZIO}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.services.{CohortTable, Zuora, ZuoraTest}
import zio.clock.Clock
import zio._

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

  def estimation(item: CohortItem, earliestStartDate: LocalDate): ZIO[Clock with Zuora, Nothing, ResultOfEstimation] = {
    val result = for {
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      account <- Zuora.fetchAccount(subscription.accountNumber)
      currentDate <- clock.currentDateTime.bimap(e => AmendmentDataFailure(e.getMessage), _.toLocalDate)
    } yield ResultOfEstimation(subscription, account, earliestStartDate, currentDate)
    result.catchAll(e => ZIO.succeed(EstimationFailed(item.subscriptionName, e)))
  }

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideCustomLayer(
        DynamoDBClient.dynamoDB >>>
        DynamoDBZIO.impl >>>
        (CohortTable.impl ++ ZuoraTest.impl)
      )
      .foldM(
        e => console.putStrLn(s"Failed: $e") *> ZIO.succeed(1),
        _ => ZIO.succeed(0)
      )
}
