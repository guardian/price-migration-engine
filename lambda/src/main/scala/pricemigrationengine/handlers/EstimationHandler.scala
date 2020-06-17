package pricemigrationengine.handlers

import java.time.{Instant, LocalDate}

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.console.Console
import zio.random.Random
import zio.{Runtime, ZEnv, ZIO, ZLayer, random}

object EstimationHandler extends zio.App with RequestHandler[Unit, Unit] {

  val main: ZIO[Logging with AmendmentConfiguration with CohortTable with Zuora with Random, Failure, Unit] =
    for {
      newProductPricing <- Zuora.fetchProductCatalogue.map(ZuoraProductCatalogue.productPricingMap)
      cohortItems <- CohortTable.fetch(ReadyForEstimation, None)
      _ <- cohortItems.foreach(estimate(newProductPricing))
    } yield ()

  private def estimate(
      newProductPricing: ZuoraPricingData
  )(item: CohortItem): ZIO[Logging with AmendmentConfiguration with CohortTable with Zuora with Random, Failure, Unit] =
    doEstimation(newProductPricing, item).foldM(
      failure = {
        case _: AmendmentDataFailure         => CohortTable.update(CohortItem(item.subscriptionName, EstimationFailed))
        case _: CancelledSubscriptionFailure => CohortTable.update(CohortItem(item.subscriptionName, Cancelled))
        case e                               => ZIO.fail(e)
      },
      success = CohortTable.update
    )

  private def doEstimation(
      newProductPricing: ZuoraPricingData,
      item: CohortItem
  ): ZIO[Logging with AmendmentConfiguration with CohortTable with Zuora with Random, Failure, CohortItem] =
    for {
      config <- AmendmentConfiguration.amendmentConfig
      subscription <- Zuora
        .fetchSubscription(item.subscriptionName)
        .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))
      invoicePreviewTargetDate = config.earliestStartDate.plusMonths(13)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId, invoicePreviewTargetDate)
      earliestStartDate <- spreadEarliestStartDate(subscription, invoicePreview)
      result <- ZIO
        .fromEither(EstimationResult(newProductPricing, subscription, invoicePreview, earliestStartDate))
        .tapBoth(
          e =>
            Logging.error(s"Failed to estimate amendment data for subscription ${subscription.subscriptionNumber}: $e"),
          result => Logging.info(s"Estimated result: $result")
        )
    } yield
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
        .flatMap(invoiceItem => ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toOption)
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
      loggingService: Logging.Service
  ): ZLayer[Any, ConfigurationFailure, Logging with AmendmentConfiguration with CohortTable with Zuora with Random] = {
    val loggingLayer = ZLayer.succeed(loggingService)
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer ++ EnvConfiguration.amendmentImpl >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.stageImp >>>
        CohortTableLive.impl
    val zuoraLayer =
      EnvConfiguration.zuoraImpl ++ loggingLayer >>>
        ZuoraLive.impl
    (loggingLayer ++ EnvConfiguration.amendmentImpl ++ cohortTableLayer ++ zuoraLayer ++ Random.live)
      .tapError(e => loggingService.error(s"Failed to create service environment: $e"))
  }

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideSomeLayer(
        env(ConsoleLogging.service(Console.Service.live))
      )
      .fold(_ => 1, _ => 0)

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.service(context))
      )
    )
}
