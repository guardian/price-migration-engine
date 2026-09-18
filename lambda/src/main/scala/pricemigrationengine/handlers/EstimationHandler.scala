package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, IO, Random, ZIO}
import pricemigrationengine.model.AmendmentEffectiveDateCalculator
import java.time.LocalDate

/** Calculates start date and new price for a set of CohortItems.
  *
  * Expected input is a CohortSpec in json format.
  *
  * Output is a HandlerOutput in json format.
  */
object EstimationHandler extends CohortHandler {

  private val batchSize = 50

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    main(input).provideSome[Logging](
      EnvConfig.zuora.layer,
      EnvConfig.stage.layer,
      DynamoDBZIOLive.impl,
      DynamoDBLive.impl,
      CohortTableLive.impl(input),
      ZuoraLive.impl
    )
  }

  def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with Zuora, Failure, HandlerOutput] =
    for {
      today <- Clock.currentDateTime.map(_.toLocalDate)
      count <- (
        cohortSpec.subscriptionNumber match {
          case None =>
            CohortTable
              .fetch(ReadyForEstimation, None)
              .take(batchSize)
          case Some(subscriptionNumber) =>
            CohortTable
              .fetch(ReadyForEstimation, None)
              .filter(item => item.subscriptionName == subscriptionNumber)
        }
      )
        .mapZIO(item =>
          processItemForEstimation(cohortSpec, item, today)
            .tapBoth(Logging.logFailure(item), Logging.logSuccess(item))
        )
        .runCount
    } yield HandlerOutput(isComplete = count < batchSize)

  private def processItemForEstimation(
      cohortSpec: CohortSpec,
      item: CohortItem,
      today: LocalDate,
  ): ZIO[Zuora with Logging with CohortTable, Failure, Unit] = {
    for {
      subscription <- Zuora.fetchSubscription(item.subscriptionName)

      estimationAnalysis = EstimationAnalysisResult.subscriptionEstimationAnalysis(cohortSpec, subscription, today)

      status <- evaluateEstimationAnalysis(
        item,
        estimationAnalysis
      )

      _ <- ZIO.when(status) {
        for {
          estimationData <- decideEstimationData(
            cohortSpec,
            item,
            subscription,
            today,
          )

          _ <- Logging.info(s"item: ${item.toString}, estimation result: ${estimationData}")

          _ <- sendEstimationDataToTable(cohortSpec, estimationData)
        } yield ()
      }

    } yield ()
  }

  /** Return a boolean indicating whether we should then pursue with
    * computing the estimation data and update the cohort item.
    */
  def evaluateEstimationAnalysis(
      item: CohortItem,
      estimationAnalysis: EstimationAnalysisResult
  ): ZIO[Zuora with Logging with CohortTable, Failure, Boolean] = {

    // Note that in some cases we ZIO.fail which interrupts the handler. In those cases
    // we want to alert and investigate before pursuing with other items.

    estimationAnalysis match {
      case EARSubscriptionCancelled => {
        for {
          _ <- Logging.error(s"subscription ${item.subscriptionName} has been cancelled in Zuora")
          _ <- CohortTable
            .update(
              CohortItem(
                item.subscriptionName,
                processingStage = ZuoraCancellation
              )
            )
        } yield false
      }

      case EARSubscriptionAutoRenewFlagFalse => {
        for {
          _ <- Logging.error(s"subscription ${item.subscriptionName} autoRenew flag is false")
          _ <- CohortTable.update(
            CohortItem(
              item.subscriptionName,
              processingStage = ExcludedFromMigration,
              cancellationReason = Some("(cause: fa6c75cb) subscription autoRenew flag is set to false")
            )
          )
        } yield false
      }

      case EARMissingData =>
        ZIO.fail(EstimationHandlerFailure(s"[cfe5c48e] EARMissingData for subscription ${item.subscriptionName}"))

      case EARPrintWithZeroBillingPeriods =>
        ZIO.fail(
          EstimationHandlerFailure(
            s"[fb51e3b0] EARPrintWithZeroBillingPeriods for subscription ${item.subscriptionName} (active rate plan with no billing period 🤔)"
          )
        )

      case EARPrintWithMoreThanTwoBillingPeriods => {
        for {
          _ <- Logging.error(s"[3fdd40ce] EARPrintWithTwoBillingPeriods for subscription ${item.subscriptionName}")
          _ <- CohortTable.update(
            CohortItem(
              item.subscriptionName,
              processingStage = ExcludedFromMigration,
              cancellationReason = Some(
                "(cause: 56b80063) active rate plan on print subscription was found with more than two billing periods"
              )
            )
          )
        } yield false
      }

      case EARClearance => ZIO.succeed(true) // will be moving to the next section
    }
  }

  def decideEstimationData(
      cohortSpec: CohortSpec,
      item: CohortItem,
      subscription: ZuoraSubscription,
      today: LocalDate,
  ): ZIO[Zuora with Logging, Failure, EstimationData] = {
    for {
      account <- Zuora.fetchAccount(subscription.accountNumber, subscription.subscriptionNumber)
      invoicePreviewTargetDate = EstimationHandlerHelper.earliestAmendmentEffectiveDate(cohortSpec).plusMonths(16)
      invoicePreview <- Zuora
        .fetchInvoicePreview(subscription.accountId, invoicePreviewTargetDate)
      amendmentEffectiveDateLowerBound <- ZIO.succeed(
        AmendmentEffectiveDateCalculator.amendmentEffectiveDateLowerBound(
          item,
          subscription,
          invoicePreview,
          cohortSpec,
          today
        )
      )
      _ <- Logging.info(
        s"item: ${item.toString}, amendmentEffectiveDateLowerBound: ${amendmentEffectiveDateLowerBound}"
      )
      estimationData <- ZIO.fromEither(
        EstimationResult(account, subscription, invoicePreview, amendmentEffectiveDateLowerBound, cohortSpec, today)
      )
      _ <- Logging.info(s"item: ${item.toString}, estimation data: ${estimationData}")
    } yield estimationData
  }

  def sendEstimationDataToTable(
      cohortSpec: CohortSpec,
      estimationData: EstimationData
  ): ZIO[CohortTable with Logging, Failure, Unit] = {
    val cohortItemZ: zio.UIO[CohortItem] =
      MigrationType(cohortSpec) match {
        case ProductMigration2025N4 => {
          // For N4 we expect the estimated new price to be equal to the old price
          // We are not performing a NoPriceIncreaseEstimationResult
          CohortItem.fromSuccessfulEstimationResult(estimationData)
        }
        case _ => {
          if (estimationData.newPriceFull <= estimationData.oldPrice)
            CohortItem.fromNoPriceIncreaseEstimationResult(estimationData)
          else CohortItem.fromSuccessfulEstimationResult(estimationData)
        }
      }
    for {
      cohortItem <- cohortItemZ
      _ <- CohortTable.update(cohortItem)
    } yield ()
  }
}
