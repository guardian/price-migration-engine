package pricemigrationengine.handlers

import java.time.{Instant, LocalDate}

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.{EstimationComplete, ReadyForEstimation}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.console.Console
import zio.random.Random
import zio.{App, Runtime, ZEnv, ZIO, ZLayer, console, random}

object EstimationHandler extends App with RequestHandler[Unit, Unit] {

  val main: ZIO[Logging with AmendmentConfiguration with CohortTable with Zuora with Random, Failure, Unit] =
    for {
      newProductPricing <- Zuora.fetchProductCatalogue.map(ZuoraProductCatalogue.productPricingMap)
      cohortItems <- CohortTable.fetch(ReadyForEstimation)
      _ <- cohortItems.foreach(writeEstimation(newProductPricing))
    } yield ()

  private def writeEstimation(
      newProductPricing: ZuoraPricingData
  )(item: CohortItem): ZIO[Logging with AmendmentConfiguration with CohortTable with Zuora with Random, Failure, Unit] =
    for {
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId)
      earliestStartDate <- spreadEarliestStartDate(subscription, invoicePreview)
      result <- ZIO
        .fromEither(EstimationResult(newProductPricing, subscription, invoicePreview, earliestStartDate))
        .tapBoth(
          e =>
            Logging.error(s"Failed to estimate amendment data for subscription ${subscription.subscriptionNumber}: $e"),
          result => Logging.info(s"Estimated result: $result")
        )
      _ <- CohortTable
        .update(
          CohortItem(
            result.subscriptionName,
            processingStage = EstimationComplete,
            oldPrice = Some(result.oldPrice),
            estimatedNewPrice = Some(result.estimatedNewPrice),
            currency = Some(result.currency),
            startDate = Some(result.startDate),
            billingPeriod = Some(result.billingPeriod),
            whenEstimationDone = Some(Instant.now())
          )
        )
        .tapBoth(
          e => Logging.error(s"Failed to update Cohort table: $e"),
          _ => Logging.info(s"Wrote $result to Cohort table")
        )
    } yield ()

  /*
   * Earliest start date spread out over 3 months.
   */
  private[handlers] def spreadEarliestStartDate(
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList
  ): ZIO[AmendmentConfiguration with Random, ConfigurationFailure, LocalDate] = {

    def earliestStartDateForAMonthlySub(config: AmendmentConfig) =
      for {
        randomFactor <- random.nextIntBetween(0, 3)
        earliestStartDate = config.earliestStartDate.plusMonths(randomFactor)
      } yield earliestStartDate

    val isMonthlySubscription =
      invoicePreview.invoiceItems
        .flatMap(ZuoraRatePlanCharge.matchingRatePlanCharge(subscription))
        .flatMap(_.billingPeriod)
        .headOption
        .contains("Month")

    AmendmentConfiguration.amendmentConfig flatMap { config =>
      if (isMonthlySubscription)
        earliestStartDateForAMonthlySub(config)
      else
        ZIO.succeed(config.earliestStartDate)
    }
  }

  private def env(
      loggingLayer: ZLayer[Any, Nothing, Logging]
  ): ZLayer[Any, Any, Logging with AmendmentConfiguration with CohortTable with Zuora with Random] = {
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer ++ EnvConfiguration.amendmentImpl >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.stageImp >>>
        CohortTableLive.impl
    val zuoraLayer =
      EnvConfiguration.zuoraImpl ++ loggingLayer >>>
        ZuoraLive.impl
    loggingLayer ++ EnvConfiguration.amendmentImpl ++ cohortTableLayer ++ zuoraLayer ++ Random.live
  }

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideSomeLayer(
        env(Console.live >>> ConsoleLogging.impl)
      )
      // output any failures in service construction - there's probably a better way to do this
      .foldM(
        e => console.putStrLn(s"Failed: $e") *> ZIO.succeed(1),
        _ => console.putStrLn("Succeeded!") *> ZIO.succeed(0)
      )

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.impl(context))
      )
    )
}
