package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, IO, Random, ZIO}
import pricemigrationengine.libs.{Date, StartDates}
import java.time.LocalDate

/** Calculates start date and new price for a set of CohortItems.
  *
  * Expected input is a CohortSpec in json format.
  *
  * Output is a HandlerOutput in json format.
  */
object EstimationHandler extends CohortHandler {

  private val batchSize = 50

  def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with Zuora, Failure, HandlerOutput] =
    for {
      today <- Clock.currentDateTime.map(_.toLocalDate)
      _ <- monitorDoNotProcessUntils(today)
      catalogue <- Zuora.fetchProductCatalogue
      count <- CohortTable
        .fetch(ReadyForEstimation, None)
        .take(batchSize)
        .mapZIO(item =>
          estimate(catalogue, cohortSpec)(today, item).tapBoth(Logging.logFailure(item), Logging.logSuccess(item))
        )
        .runCount
        .tapError(e => Logging.error(e.toString))
    } yield HandlerOutput(isComplete = count < batchSize)

  def monitorDoNotProcessUntilItem(today: LocalDate, item: CohortItem): ZIO[Logging with CohortTable, Failure, Unit] = {
    for {
      _ <-
        if (CohortItem.isProcessable(item, today)) {
          CohortTable
            .update(
              CohortItem(
                subscriptionName = item.subscriptionName,
                processingStage = ReadyForEstimation
              )
            )
        } else { ZIO.succeed(()) }
    } yield ()
  }

  def monitorDoNotProcessUntils(today: LocalDate): ZIO[Logging with CohortTable, Failure, Unit] = {
    // This function migrates items in DoNotProcessUntil state, which have passed their
    // expiration time, to ReadyForEstimation
    for {
      _ <- CohortTable
        .fetch(DoNotProcessUntil, None)
        .foreach { item =>
          for {
            _ <- Logging.info(s"item in DoNotShowUntil stage: ${item.toString}")
            _ <- monitorDoNotProcessUntilItem(today, item)
          } yield ()
        }
    } yield ()
  }

  private[handlers] def estimate(
      catalogue: ZuoraProductCatalogue,
      cohortSpec: CohortSpec
  )(
      today: LocalDate,
      item: CohortItem,
  ): ZIO[CohortTable with Zuora, Failure, EstimationResult] =
    doEstimation(catalogue, item, cohortSpec, today).foldZIO(
      failure = {
        case failure: DataExtractionFailure =>
          val result = FailedEstimationResult(item.subscriptionName, failure.reason)
          CohortTable.update(CohortItem.fromFailedEstimationResult(result)).as(result)
        case _: CancelledSubscriptionFailure =>
          val result = CancelledEstimationResult(item.subscriptionName)
          CohortTable
            .update(
              CohortItem.fromCancelledEstimationResult(
                result,
                s"(reason: b6829dd30) subscription ${item.subscriptionName} has been cancelled in Zuora"
              )
            )
            .as(result)
        case e => ZIO.fail(e)
      },
      success = { result =>
        val cohortItemToWrite =
          if (result.estimatedNewPrice <= result.oldPrice) CohortItem.fromNoPriceIncreaseEstimationResult(result)
          else CohortItem.fromSuccessfulEstimationResult(result)

        for {
          cohortItem <- cohortItemToWrite
          _ <- CohortTable.update(cohortItem)
        } yield result
      }
    )

  private def doEstimation(
      catalogue: ZuoraProductCatalogue,
      item: CohortItem,
      cohortSpec: CohortSpec,
      today: LocalDate,
  ): ZIO[Zuora, Failure, EstimationData] = {
    for {
      subscription <-
        Zuora
          .fetchSubscription(item.subscriptionName)
          .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))
      account <- Zuora.fetchAccount(subscription.accountNumber, subscription.subscriptionNumber)
      invoicePreviewTargetDate = cohortSpec.earliestPriceMigrationStartDate.plusMonths(16)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId, invoicePreviewTargetDate)
      startDateLowerBound <- ZIO.succeed(
        StartDates.startDateLowerBound(
          item: CohortItem,
          subscription,
          invoicePreview,
          cohortSpec,
          today
        )
      )
      _ <- ZIO.logInfo(s"item: ${item.toString}, startDateLowerBound: ${startDateLowerBound}")
      result <- ZIO.fromEither(
        EstimationResult(account, catalogue, subscription, invoicePreview, startDateLowerBound, cohortSpec)
      )
      _ <- ZIO.logInfo(s"item: ${item.toString}, estimation result: ${result}")
    } yield result
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    main(input).provideSome[Logging](
      EnvConfig.cohortTable.layer,
      EnvConfig.zuora.layer,
      EnvConfig.stage.layer,
      DynamoDBZIOLive.impl,
      DynamoDBClientLive.impl,
      CohortTableLive.impl(input),
      ZuoraLive.impl
    )
  }
}
