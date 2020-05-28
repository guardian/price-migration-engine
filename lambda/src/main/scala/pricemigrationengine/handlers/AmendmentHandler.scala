package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.SalesforcePriceRiceCreationComplete
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
      cohortItems <- CohortTable.fetch(SalesforcePriceRiceCreationComplete)
      _ <- cohortItems.foreach(amend)
    } yield ()

  private def amend(
      item: CohortItem
  ): ZIO[Logging with AmendmentConfiguration with CohortTable with Zuora, Failure, Unit] =
    for {
      config <- AmendmentConfiguration.amendmentConfig
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      invoicePreviewBeforeUpdate <- Zuora.fetchInvoicePreview(subscription.accountId)
      startDate <- ZIO.fromEither(
        AmendmentData.nextBillingDate(invoicePreviewBeforeUpdate, config.earliestStartDate)
      )
      update <- ZIO.fromEither(
        ZuoraSubscriptionUpdate.updateOfRatePlansToCurrent(subscription, invoicePreviewBeforeUpdate, startDate)
      )
      newSubscriptionId <- Zuora.updateSubscription(subscription, update)
      invoicePreviewAfterUpdate <- Zuora.fetchInvoicePreview(subscription.accountId)
      totalChargeAmount <- ZIO
        .fromEither(AmendmentData.totalChargeAmount(subscription, invoicePreviewAfterUpdate, startDate))
        .mapError(
          e =>
            AmendmentDataFailure(
              s"Failed to calculate amendment of subscription ${subscription.subscriptionNumber}: $e"
          )
        )
      result = AmendmentResult(
        subscription.subscriptionNumber,
        startDate,
        totalChargeAmount,
        newSubscriptionId
      )
      _ <- CohortTable.update(result)
    } yield ()

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
