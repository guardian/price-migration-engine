package pricemigrationengine.handlers

import java.io.{InputStream, OutputStream}
import java.time.{Instant, LocalDate}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.services._
import ujson.Readable
import upickle.default.{read, stream}
import zio.console.Console
import zio.random.Random
import zio.{ExitCode, Runtime, ZEnv, ZIO, ZLayer, random}

/**
  * Calculates start date and new price for a set of CohortItems.
  *
  * Expected input is a CohortSpec in json format.
  *
  * Output is a HandlerOutput in json format.
  */
object EstimationHandler extends zio.App with RequestStreamHandler {

  // TODO: move to config
  private val batchSize = 100

  def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with Zuora with Random, Failure, HandlerOutput] =
    for {
      newProductPricing <- Zuora.fetchProductCatalogue.map(ZuoraProductCatalogue.productPricingMap)
      cohortItems <- CohortTable.fetch(ReadyForEstimation, None)
      _ <- cohortItems.take(batchSize).foreach(estimate(newProductPricing, cohortSpec.earliestPriceMigrationStartDate))
      itemsToGo <- CohortTable.fetch(ReadyForEstimation, None)
      numItemsToGo <- itemsToGo.take(1).runCount
    } yield HandlerOutput(cohortSpec, isComplete = numItemsToGo == 0)

  private def estimate(newProductPricing: ZuoraPricingData, earliestStartDate: LocalDate)(
      item: CohortItem
  ): ZIO[Logging with CohortTable with Zuora with Random, Failure, Unit] =
    doEstimation(newProductPricing, item, earliestStartDate).foldM(
      failure = {
        case _: AmendmentDataFailure         => CohortTable.update(CohortItem(item.subscriptionName, EstimationFailed))
        case _: CancelledSubscriptionFailure => CohortTable.update(CohortItem(item.subscriptionName, Cancelled))
        case e                               => ZIO.fail(e)
      },
      success = CohortTable.update
    )

  private def doEstimation(
      newProductPricing: ZuoraPricingData,
      item: CohortItem,
      earliestStartDate: LocalDate
  ): ZIO[Logging with CohortTable with Zuora with Random, Failure, CohortItem] =
    for {
      subscription <-
        Zuora
          .fetchSubscription(item.subscriptionName)
          .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))
      invoicePreviewTargetDate = earliestStartDate.plusMonths(13)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId, invoicePreviewTargetDate)
      earliestStartDate <- spreadEarliestStartDate(subscription, invoicePreview, earliestStartDate)
      result <-
        ZIO
          .fromEither(EstimationResult(newProductPricing, subscription, invoicePreview, earliestStartDate))
          .tapBoth(
            e =>
              Logging
                .error(s"Failed to estimate amendment data for subscription ${subscription.subscriptionNumber}: $e"),
            result => Logging.info(s"Estimated result: $result")
          )
    } yield CohortItem(
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
      invoicePreview: ZuoraInvoiceList,
      earliestStartDate: LocalDate
  ): ZIO[Random, ConfigurationFailure, LocalDate] = {

    lazy val earliestStartDateForAMonthlySub =
      for {
        randomFactor <- random.nextIntBetween(0, 3)
        actualEarliestStartDate = earliestStartDate.plusMonths(randomFactor)
      } yield actualEarliestStartDate

    val isMonthlySubscription =
      invoicePreview.invoiceItems
        .flatMap(invoiceItem => ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toOption)
        .flatMap(_.billingPeriod)
        .headOption
        .contains("Month")

    if (isMonthlySubscription)
      earliestStartDateForAMonthlySub
    else
      ZIO.succeed(earliestStartDate)
  }

  private def env(
      loggingService: Logging.Service
  ): ZLayer[Any, ConfigurationFailure, Logging with CohortTable with Zuora with Random] = {
    val loggingLayer = ZLayer.succeed(loggingService)
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl andTo
        DynamoDBClient.dynamoDB andTo
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.stageImp andTo
        CohortTableLive.impl
    val zuoraLayer =
      EnvConfiguration.zuoraImpl ++ loggingLayer >>>
        ZuoraLive.impl
    (loggingLayer ++ cohortTableLayer ++ zuoraLayer ++ Random.live)
      .tapError(e => loggingService.error(s"Failed to create service environment: $e"))
  }

  private def go(loggingService: Logging.Service, input: Readable): ZIO[ZEnv, Failure, HandlerOutput] =
    (for {
      cohortSpec <-
        ZIO
          .effect(Option(read[CohortSpec](input)))
          .mapError(e => InputFailure(s"Failed to parse json: $e"))
          .filterOrElse(_.exists(CohortSpec.isValid))(spec => ZIO.fail(InputFailure(s"Invalid cohort spec: $spec")))
          .tap(spec => loggingService.info(s"Input: $spec"))
      validSpec <- ZIO.fromOption(cohortSpec).orElseFail(InputFailure("No input"))
      output <- main(validSpec).provideCustomLayer(env(loggingService))
    } yield output).tapBoth(
      e => loggingService.error(e.toString),
      output => loggingService.info(s"Output: $output")
    )

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    (for {
      input <- ZIO.fromOption(args.headOption).orElseFail(InputFailure("No input"))
      _ <- go(
        loggingService = ConsoleLogging.service(Console.Service.live),
        input = input
      )
    } yield ()).exitCode

  def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    val program = for {
      handlerOutput <- go(
        loggingService = LambdaLogging.service(context),
        input
      )
      writable <- ZIO.effect(stream(handlerOutput))
      _ <- ZIO.effect(writable.writeBytesTo(output))
    } yield ()
    Runtime.default.unsafeRun(program)
  }
}
