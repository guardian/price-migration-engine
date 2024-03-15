package pricemigrationengine.handlers

import pricemigrationengine.migrations.newspaper2024Migration
import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, IO, Random, ZIO}
import pricemigrationengine.util.{Date, StartDates}
import java.time.LocalDate

/** Calculates start date and new price for a set of CohortItems.
  *
  * Expected input is a CohortSpec in json format.
  *
  * Output is a HandlerOutput in json format.
  */
object EstimationHandler extends CohortHandler {

  private val batchSize = 150

  def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with Zuora, Failure, HandlerOutput] =
    for {
      today <- Clock.currentDateTime.map(_.toLocalDate)
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

  private[handlers] def estimate(
      catalogue: ZuoraProductCatalogue,
      cohortSpec: CohortSpec
  )(
      today: LocalDate,
      item: CohortItem,
  ): ZIO[CohortTable with Zuora, Failure, EstimationResult] =
    doEstimation(catalogue, item, cohortSpec, today).foldZIO(
      failure = {
        case failure: AmendmentDataFailure =>
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
        val cohortItemToWrite = MigrationType(cohortSpec) match {
          case SupporterPlus2023V1V2MA => {
            // SupporterPlus2023V1V2 is different here, because it's a rate plan migration and not a price increase
            // The first of its kind. In particular we do not want processing stage `NoPriceIncrease`
            CohortItem.fromSuccessfulEstimationResult(result)
          }
          case _ => {
            if (result.estimatedNewPrice <= result.oldPrice) CohortItem.fromNoPriceIncreaseEstimationResult(result)
            else CohortItem.fromSuccessfulEstimationResult(result)
          }
        }

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
      startDateLowerBound <- StartDates.startDateLowerBound(
        subscription,
        invoicePreview,
        cohortSpec,
        today
      )
      result <- ZIO.fromEither(
        EstimationResult(account, catalogue, subscription, invoicePreview, startDateLowerBound, cohortSpec)
      )
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
