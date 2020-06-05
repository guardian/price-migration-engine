package pricemigrationengine.handlers

import java.time.Instant

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.{AmendmentComplete, Cancelled, SalesforcePriceRiceCreationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.console.Console
import zio.{App, Runtime, ZEnv, ZIO, ZLayer, console}

/**
  * Carries out price-rise amendments in Zuora.
  */
object AmendmentHandler extends App with RequestHandler[Unit, Unit] {

  val main: ZIO[Logging with AmendmentConfiguration with CohortTable with Zuora, Failure, Unit] =
    for {
      cohortItems <- CohortTable.fetch(SalesforcePriceRiceCreationComplete, None)
      _ <- cohortItems.foreach(amend)
    } yield ()

  private def amend(
      item: CohortItem
  ): ZIO[Logging with AmendmentConfiguration with CohortTable with Zuora, Failure, Unit] =
    doAmendment(item).foldM(
      failure = {
        case _: CancelledSubscriptionFailure => CohortTable.update(CohortItem(item.subscriptionName, Cancelled))
        case e                               => ZIO.fail(e)
      },
      success = CohortTable.update
    )

  private def doAmendment(
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
          .updateOfRatePlansToCurrent(subscriptionBeforeUpdate, invoicePreviewBeforeUpdate, startDate)
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
      .filterOrFail(_.status == "Active")(CancelledSubscriptionFailure(item.subscriptionName))

  private def env(
      loggingLayer: ZLayer[Any, Nothing, Logging]
  ) = {
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer ++ EnvConfiguration.amendmentImpl >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.stageImp >>>
        CohortTableLive.impl
    val zuoraLayer =
      EnvConfiguration.zuoraImpl ++ loggingLayer >>>
        ZuoraLive.impl
    loggingLayer ++ EnvConfiguration.amendmentImpl ++ cohortTableLayer ++ zuoraLayer
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
