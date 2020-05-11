package pricemigrationengine.handlers

import java.time.LocalDate

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.ReadyForEstimation
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{App, Runtime, ZEnv, ZIO}

object EstimationHandler extends App with RequestHandler[Unit, Unit] {

  // TODO: configuration
  val batchSize = 100
  val earliestStartDate = LocalDate.now

  def estimation(
      newProductPricing: ZuoraPricingData,
      earliestStartDate: LocalDate,
      item: CohortItem
  ): ZIO[Zuora, Failure, EstimationResult] = {
    val result = for {
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId)
    } yield EstimationResult(newProductPricing, subscription, invoicePreview, earliestStartDate)
    result.absolve
  }

  val main: ZIO[Logging with CohortTable with Zuora, Failure, Unit] =
    for {
      newProductPricing <- Zuora.fetchProductCatalogue.map(ZuoraProductCatalogue.productPricingMap)
      cohortItems <- CohortTable
        .fetch(ReadyForEstimation, batchSize)
      results = cohortItems.mapM(
        item =>
          estimation(newProductPricing, earliestStartDate, item).tapBoth(
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

  private def env(logging: ZLayer[Any, Nothing, Logging] ): ZLayer[Any, Any, Logging with CohortTable with Zuora] =
    logging >>>
    DynamoDBClient.dynamoDB ++ logging >>>
    DynamoDBZIOLive.impl ++ logging >>>
    logging ++ CohortTableLive.impl ++ ZuoraTest.impl

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideSomeLayer(
        env(Console.live >>> ConsoleLogging.impl)
      )
      .fold(_ => 1, _ => 0)

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.impl(context))
      )
    )
}
