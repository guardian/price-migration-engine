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

  def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with Zuora, Failure, HandlerOutput] =
    for {
      // DoNotProcessUntil
      today <- Clock.currentDateTime.map(_.toLocalDate)
      _ <- CohortTable
        .fetch(DoNotProcessUntil, None)
        .foreach { item =>
          for {
            _ <- Logging.info(s"[1beb60af] DoNotProcessUntil: about to check: ${item.toString}")
            ok = ItemHibernation.isProcessable(item, today)
            _ <- ZIO.when(ok)(for {
              _ <- Logging.info(
                s"[83433310] DoNotProcessUntil: today is ${today.toString}, moving ${item.toString} back to ReadyForEstimation"
              )
              _ <- CohortTable
                .update(
                  CohortItem(
                    subscriptionName = item.subscriptionName,
                    processingStage = ReadyForEstimation
                  )
                )
            } yield ZIO.succeed(()))
          } yield ()
        }
      // Estimation
      catalogue <- Zuora.fetchProductCatalogue
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
  ): ZIO[CohortTable with Zuora with Logging, Failure, EstimationResult] =
    doEstimation(catalogue, item, cohortSpec, today).foldZIO(
      failure = {
        case _: SubscriptionCancelledInZuoraFailure =>
          val result = SubscriptionCancelledInZuoraEstimationResult(item.subscriptionName)
          CohortTable
            .update(
              CohortItem(
                item.subscriptionName,
                processingStage = ZuoraCancellation
              )
            )
            .as(result)
        case _: ZuoraEmptyInvoiceFailure =>
          val result = EmptyInvoicePreviewZuoraEstimationResult(item.subscriptionName)
          CohortTable
            .update(
              CohortItem(
                item.subscriptionName,
                processingStage = ZuoraEmptyInvoicePreview
              )
            )
            .as(result)
        case e => ZIO.fail(e)
      },
      success = { result =>
        val cohortItemToWrite =
          MigrationType(cohortSpec) match {
            case ProductMigration2025N4 => {
              // For N4 we expect the estimated new price to be equal to the old price
              // We are not performing a NoPriceIncreaseEstimationResult
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
  ): ZIO[Zuora with Logging, Failure, EstimationData] = {
    for {
      subscription <-
        Zuora
          .fetchSubscription(item.subscriptionName)
          .filterOrFail(_.status != "Cancelled")(
            SubscriptionCancelledInZuoraFailure(s"subscription ${item.subscriptionName} has been cancelled in Zuora")
          )
      account <- Zuora.fetchAccount(subscription.accountNumber, subscription.subscriptionNumber)
      invoicePreviewTargetDate = cohortSpec.earliestAmendmentEffectiveDate.plusMonths(16)
      invoicePreview <- Zuora
        .fetchInvoicePreview(subscription.accountId, invoicePreviewTargetDate)
      _ <-
        if (invoicePreview.invoiceItems.isEmpty) {
          // This check was added after discovering the existence of active subscriptions
          // with expired active rate plan, whose invoice previews are empty
          ZIO.fail(
            ZuoraEmptyInvoiceFailure(s"[ea4e9328] subscription ${item.subscriptionName} has an empty invoice preview")
          )
        } else {
          ZIO.succeed(())
        }
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
      result <- ZIO.fromEither(
        EstimationResult(account, catalogue, subscription, invoicePreview, amendmentEffectiveDateLowerBound, cohortSpec)
      )
      _ <- Logging.info(s"item: ${item.toString}, estimation result: ${result}")
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
