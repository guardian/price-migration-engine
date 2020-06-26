package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.{AmendmentComplete, AmendmentWrittenToSalesforce}
import pricemigrationengine.model.{CohortItem, Failure, SalesforcePriceRise, SalesforcePriceRiseWriteFailure}
import pricemigrationengine.services._
import zio.clock.Clock
import zio.console.Console
import zio.{ExitCode, IO, Runtime, ZEnv, ZIO, ZLayer}

/**
  * Updates Salesforce with evidence of the price-rise amendment that was applied in Zuora.
  */
object SalesforceAmendmentUpdateHandler extends zio.App with RequestHandler[Unit, Unit] {

  private val main: ZIO[CohortTable with SalesforceClient with Clock with Logging, Failure, Unit] =
    for {
      cohortItems <- CohortTable.fetch(AmendmentComplete, None)
      _ <- cohortItems.foreach(updateSfWithNewSubscriptionId)
    } yield ()

  private def updateSfWithNewSubscriptionId(
      item: CohortItem
  ): ZIO[CohortTable with SalesforceClient with Clock with Logging, Failure, Unit] =
    for {
      priceRise <- ZIO.fromEither(buildPriceRise(item))
      salesforcePriceRiseId <- IO
        .fromOption(item.salesforcePriceRiseId)
        .orElseFail(
          SalesforcePriceRiseWriteFailure(
            s"${item.subscriptionName}: salesforcePriceRiseId is required to update Salesforce"
          ))
      _ <- SalesforceClient
        .updatePriceRise(salesforcePriceRiseId, priceRise)
        .tapBoth(
          e => Logging.error(s"${item.subscriptionName}: failed to update Salesforce: $e"),
          _ => Logging.info(s"Amendment of ${item.subscriptionName} recorded in Salesforce")
        )
      now <- Time.thisInstant
      _ <- CohortTable
        .update(
          CohortItem(
            subscriptionName = item.subscriptionName,
            processingStage = AmendmentWrittenToSalesforce,
            whenAmendmentWrittenToSalesforce = Some(now)
          ))
    } yield ()

  private def buildPriceRise(
      cohortItem: CohortItem
  ): Either[SalesforcePriceRiseWriteFailure, SalesforcePriceRise] =
    cohortItem.newSubscriptionId
      .map(newSubscriptionId => SalesforcePriceRise(Amended_Zuora_Subscription_Id__c = Some(newSubscriptionId)))
      .toRight(SalesforcePriceRiseWriteFailure(s"$cohortItem does not have a newSubscriptionId field"))

  private def env(
      loggingService: Logging.Service
  ): ZLayer[Any, Any, Logging with CohortTable with SalesforceClient with Clock] = {
    val loggingLayer = ZLayer.succeed(loggingService)
    loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
      DynamoDBClient.dynamoDB ++ loggingLayer >>>
      DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++ EnvConfiguration.stageImp ++ EnvConfiguration.salesforceImp >>>
      (loggingLayer ++ CohortTableLive.impl ++ SalesforceClientLive.impl ++ Clock.live)
        .tapError(e => loggingService.error(s"Failed to create service environment: $e"))
  }

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    main
      .provideSomeLayer(
        env(ConsoleLogging.service(Console.Service.live))
      )
      .exitCode

  def handleRequest(unused: Unit, context: Context): Unit =
    Runtime.default.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.service(context))
      )
    )
}
