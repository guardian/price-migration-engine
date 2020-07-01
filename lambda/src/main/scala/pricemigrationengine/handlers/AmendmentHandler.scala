package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.NotificationSendDateWrittenToSalesforce
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.clock.Clock
import zio.console.Console
import zio.{App, ExitCode, Runtime, ZEnv, ZIO, ZLayer}

/**
  * Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends App with RequestHandler[Unit, Unit] {

  val main: ZIO[Logging with CohortTable with Zuora with Clock, Failure, Unit] =
    for {
      newProductPricing <- Zuora.fetchProductCatalogue.map(ZuoraProductCatalogue.productPricingMap)
      cohortItems <- CohortTable
        .fetch(NotificationSendDateWrittenToSalesforce, None)
      _ <- cohortItems.foreach(
        item =>
          amend(newProductPricing, item).tapBoth(
            Logging.logFailure(item),
            Logging.logSuccess(item)
        ))
    } yield ()

  private def amend(newProductPricing: ZuoraPricingData,
                    item: CohortItem): ZIO[CohortTable with Zuora with Clock, Failure, AmendmentResult] =
    for {
      result <- doAmendment(newProductPricing, item).foldM(
        failure = {
          case _: CancelledSubscriptionFailure =>
            val result = CancelledAmendmentResult(item.subscriptionName)
            CohortTable.update(CohortItem.fromCancelledAmendmentResult(result)) zipRight ZIO.succeed(result)
          case e => ZIO.fail(e)
        },
        success = { result =>
          CohortTable.update(
            CohortItem.fromSuccessfulAmendmentResult(result)
          ) zipRight ZIO.succeed(result)
        }
      )
    } yield result

  private def doAmendment(newProductPricing: ZuoraPricingData,
                          item: CohortItem): ZIO[Zuora with Clock, Failure, SuccessfulAmendmentResult] =
    for {
      startDate <- ZIO.fromOption(item.startDate).orElseFail(AmendmentDataFailure(s"No start date in $item"))
      oldPrice <- ZIO.fromOption(item.oldPrice).orElseFail(AmendmentDataFailure(s"No old price in $item"))
      estimatedNewPrice <- ZIO
        .fromOption(item.estimatedNewPrice)
        .orElseFail(AmendmentDataFailure(s"No estimated new price in $item"))
      invoicePreviewTargetDate = startDate.plusMonths(13)
      subscriptionBeforeUpdate <- fetchSubscription(item)
      invoicePreviewBeforeUpdate <- Zuora.fetchInvoicePreview(subscriptionBeforeUpdate.accountId,
                                                              invoicePreviewTargetDate)
      update <- ZIO.fromEither(ZuoraSubscriptionUpdate
        .updateOfRatePlansToCurrent(newProductPricing, subscriptionBeforeUpdate, invoicePreviewBeforeUpdate, startDate))
      newSubscriptionId <- Zuora.updateSubscription(subscriptionBeforeUpdate, update)
      subscriptionAfterUpdate <- fetchSubscription(item)
      invoicePreviewAfterUpdate <- Zuora.fetchInvoicePreview(subscriptionAfterUpdate.accountId,
                                                             invoicePreviewTargetDate)
      newPrice <- ZIO.fromEither(
        AmendmentData.totalChargeAmount(subscriptionAfterUpdate, invoicePreviewAfterUpdate, startDate))
      whenDone <- Time.thisInstant
    } yield
      SuccessfulAmendmentResult(
        item.subscriptionName,
        startDate,
        oldPrice,
        newPrice,
        estimatedNewPrice,
        newSubscriptionId,
        whenDone
      )

  private def fetchSubscription(item: CohortItem): ZIO[Zuora, Failure, ZuoraSubscription] =
    Zuora
      .fetchSubscription(item.subscriptionName)
      .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))

  private def env(
      loggingService: Logging.Service
  ) = {
    val loggingLayer = ZLayer.succeed(loggingService)
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.stageImp >>>
        CohortTableLive.impl
    val zuoraLayer =
      EnvConfiguration.zuoraImpl ++ loggingLayer >>>
        ZuoraLive.impl
    (loggingLayer ++ cohortTableLayer ++ zuoraLayer)
      .tapError(e => loggingService.error(s"Failed to create service environment: $e"))
  }

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    main
      .provideCustomLayer(
        env(ConsoleLogging.service(Console.Service.live))
      )
      .exitCode

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideCustomLayer(
        env(LambdaLogging.service(context))
      )
    )
}
