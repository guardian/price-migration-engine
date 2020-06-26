package pricemigrationengine.handlers

import java.time.Instant

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.{
  AmendmentComplete,
  Cancelled,
  NotificationSendDateWrittenToSalesforce
}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.console.Console
import zio.{App, ExitCode, Runtime, ZEnv, ZIO, ZLayer}

/**
  * Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends App with RequestHandler[Unit, Unit] {

  val main: ZIO[Logging with CohortTable with Zuora, Failure, Unit] =
    for {
      newProductPricing <- Zuora.fetchProductCatalogue.map(ZuoraProductCatalogue.productPricingMap)
      cohortItems <- CohortTable.fetch(NotificationSendDateWrittenToSalesforce, None)
      _ <- cohortItems.foreach(amend(newProductPricing))
    } yield ()

  private def amend(newProductPricing: ZuoraPricingData)(
      item: CohortItem
  ): ZIO[Logging with CohortTable with Zuora, Failure, Unit] =
    doAmendment(newProductPricing, item).foldM(
      failure = {
        case _: CancelledSubscriptionFailure => CohortTable.update(CohortItem(item.subscriptionName, Cancelled))
        case e                               => ZIO.fail(e)
      },
      success = CohortTable.update
    )

  private def doAmendment(
      newProductPricing: ZuoraPricingData,
      item: CohortItem
  ): ZIO[Logging with Zuora, Failure, CohortItem] =
    for {
      startDate <- ZIO.fromOption(item.startDate).orElseFail(AmendmentDataFailure(s"No start date in $item"))
      invoicePreviewTargetDate = startDate.plusMonths(13)
      subscriptionBeforeUpdate <- fetchSubscription(item)
      invoicePreviewBeforeUpdate <- Zuora.fetchInvoicePreview(
        subscriptionBeforeUpdate.accountId,
        invoicePreviewTargetDate
      )
      update <- ZIO.fromEither(
        ZuoraSubscriptionUpdate
          .updateOfRatePlansToCurrent(
            newProductPricing,
            subscriptionBeforeUpdate,
            invoicePreviewBeforeUpdate,
            startDate
          )
      )
      newSubscriptionId <- Zuora.updateSubscription(subscriptionBeforeUpdate, update)
      subscriptionAfterUpdate <- fetchSubscription(item)
      invoicePreviewAfterUpdate <- Zuora.fetchInvoicePreview(
        subscriptionAfterUpdate.accountId,
        invoicePreviewTargetDate
      )
      totalChargeAmount <- ZIO
        .fromEither(AmendmentData.totalChargeAmount(subscriptionAfterUpdate, invoicePreviewAfterUpdate, startDate))
        .mapError(
          e =>
            AmendmentDataFailure(
              s"Failed to calculate amendment of subscription ${subscriptionBeforeUpdate.subscriptionNumber}: $e"
          )
        )
        .tap(newPrice =>
          Logging.info(
            s"Amendment made: ${item.subscriptionName}: price change on $startDate: ${item.oldPrice} -> $newPrice (estimated ${item.estimatedNewPrice})"))
    } yield
      CohortItem(
        subscriptionBeforeUpdate.subscriptionNumber,
        processingStage = AmendmentComplete,
        startDate = Option(startDate),
        newPrice = Some(totalChargeAmount),
        newSubscriptionId = Some(newSubscriptionId),
        whenAmendmentDone = Some(Instant.now())
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
      .provideSomeLayer(
        env(ConsoleLogging.service(Console.Service.live))
      )
      .exitCode

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.service(context))
      )
    )
}
