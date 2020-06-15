package pricemigrationengine.handlers

import java.time.Instant

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.{AmendmentComplete, Cancelled, SalesforcePriceRiceCreationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.console.Console
import zio.{App, Runtime, ZEnv, ZIO, ZLayer}

/**
  * Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends App with RequestHandler[Unit, Unit] {

  val main: ZIO[Logging with AmendmentConfiguration with CohortTable with Zuora, Failure, Unit] =
    for {
      newProductPricing <- Zuora.fetchProductCatalogue.map(ZuoraProductCatalogue.productPricingMap)
      cohortItems <- CohortTable.fetch(SalesforcePriceRiceCreationComplete, None)
      _ <- cohortItems.foreach(amend(newProductPricing))
    } yield ()

  private def amend(newProductPricing: ZuoraPricingData)(
      item: CohortItem
  ): ZIO[Logging with AmendmentConfiguration with CohortTable with Zuora, Failure, Unit] =
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
  ): ZIO[Logging with AmendmentConfiguration with Zuora, Failure, CohortItem] =
    for {
      config <- AmendmentConfiguration.amendmentConfig
      invoicePreviewTargetDate = config.earliestStartDate.plusMonths(13)
      subscriptionBeforeUpdate <- fetchSubscription(item)
      invoicePreviewBeforeUpdate <- Zuora.fetchInvoicePreview(
        subscriptionBeforeUpdate.accountId,
        invoicePreviewTargetDate
      )
      startDate <- ZIO.fromOption(item.startDate).orElseFail(AmendmentDataFailure(s"No start date in $item"))
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
        DynamoDBClient.dynamoDB ++ loggingLayer ++ EnvConfiguration.amendmentImpl >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.stageImp >>>
        CohortTableLive.impl
    val zuoraLayer =
      EnvConfiguration.zuoraImpl ++ loggingLayer >>>
        ZuoraLive.impl
    (loggingLayer ++ EnvConfiguration.amendmentImpl ++ cohortTableLayer ++ zuoraLayer)
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
