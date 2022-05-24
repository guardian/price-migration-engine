package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{IO, Random, ZIO, ZLayer}

import java.time.LocalDate

/** Calculates start date and new price for a set of CohortItems.
  *
  * Expected input is a CohortSpec in json format.
  *
  * Output is a HandlerOutput in json format.
  */
object EstimationHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 150

  def main(earliestStartDate: LocalDate): ZIO[Logging with CohortTable with Zuora, Failure, HandlerOutput] =
    for {
      catalogue <- Zuora.fetchProductCatalogue

      cohortItems <- CohortTable.fetch(ReadyForEstimation, None)
      count <-
        cohortItems
          .take(batchSize)
          .mapZIO(item =>
            estimate(catalogue, earliestStartDate)(item)
              .tapBoth(Logging.logFailure(item), Logging.logSuccess(item))
          )
          .runCount
          .tapError(e => Logging.error(e.toString))
    } yield HandlerOutput(isComplete = count < batchSize)

  private[handlers] def estimate(
      catalogue: ZuoraProductCatalogue,
      earliestStartDate: LocalDate
  )(
      item: CohortItem
  ): ZIO[CohortTable with Zuora, Failure, EstimationResult] =
    doEstimation(catalogue, item, earliestStartDate).foldZIO(
      failure = {
        case failure: AmendmentDataFailure =>
          val result = FailedEstimationResult(item.subscriptionName, failure.reason)
          CohortTable.update(CohortItem.fromFailedEstimationResult(result)).as(result)
        case _: CancelledSubscriptionFailure =>
          val result = CancelledEstimationResult(item.subscriptionName)
          CohortTable.update(CohortItem.fromCancelledEstimationResult(result)).as(result)
        case e => ZIO.fail(e)
      },
      success = { result =>
        val cohortItemToWrite =
          if (result.estimatedNewPrice <= result.oldPrice)
            CohortItem.fromNoPriceIncreaseEstimationResult(result)
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
      earliestStartDate: LocalDate
  ): ZIO[Zuora, Failure, SuccessfulEstimationResult] =
    for {
      subscription <-
        Zuora
          .fetchSubscription(item.subscriptionName)
          .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))
      invoicePreviewTargetDate = earliestStartDate.plusMonths(13)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId, invoicePreviewTargetDate)
      earliestStartDate <- spreadEarliestStartDate(subscription, invoicePreview, earliestStartDate)
      result <- ZIO.fromEither(EstimationResult(catalogue, subscription, invoicePreview, earliestStartDate))
    } yield result

  /*
   * Earliest start date spread out over 3 months.
   */
  private[handlers] def spreadEarliestStartDate(
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList,
      earliestStartDate: LocalDate
  ): IO[ConfigFailure, LocalDate] = {

    lazy val earliestStartDateForAMonthlySub =
      for {
        randomFactor <- Random.nextIntBetween(0, 3)
        actualEarliestStartDate = earliestStartDate.plusMonths(randomFactor)
      } yield actualEarliestStartDate

    val isMonthlySubscription =
      invoicePreview.invoiceItems
        .flatMap(invoiceItem => ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toOption)
        .flatMap(_.billingPeriod)
        .headOption
        .contains("Month")

    if (isMonthlySubscription)
      earliestStartDateForAMonthlySub
    else
      ZIO.succeed(earliestStartDate)
  }

  private def env(cohortSpec: CohortSpec): ZLayer[Logging, ConfigFailure, CohortTable with Zuora with Logging] =
    (LiveLayer.cohortTable(cohortSpec) and LiveLayer.zuora and LiveLayer.logging)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] =
    main(input.earliestPriceMigrationStartDate).provideSomeLayer[Logging](env(input))
}
