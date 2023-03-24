package pricemigrationengine.handlers

import pricemigrationengine.model.CohortSpec.isMembershipPriceRiseBatch1
import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.{Clock, IO, Random, UIO, ZIO}

import java.time.LocalDate
import java.time.temporal._

/** Calculates start date and new price for a set of CohortItems.
  *
  * Expected input is a CohortSpec in json format.
  *
  * Output is a HandlerOutput in json format.
  */
object EstimationHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 150

  def main(cohortSpec: CohortSpec): ZIO[Logging with CohortTable with Zuora, Failure, HandlerOutput] =
    for {
      catalogue <- Zuora.fetchProductCatalogue
      count <- CohortTable
        .fetch(ReadyForEstimation, None)
        .take(batchSize)
        .mapZIO(item =>
          estimate(catalogue, cohortSpec)(
            item
          ).tapBoth(Logging.logFailure(item), Logging.logSuccess(item))
        )
        .runCount
        .tapError(e => Logging.error(e.toString))
    } yield HandlerOutput(isComplete = count < batchSize)

  private[handlers] def estimate(
      catalogue: ZuoraProductCatalogue,
      cohortSpec: CohortSpec
  )(
      item: CohortItem
  ): ZIO[CohortTable with Zuora, Failure, EstimationResult] =
    doEstimation(catalogue, item, cohortSpec).foldZIO(
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
      cohortSpec: CohortSpec
  ): ZIO[Zuora, Failure, SuccessfulEstimationResult] = {

    for {
      subscription <-
        Zuora
          .fetchSubscription(item.subscriptionName)
          .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))
      account <- Zuora.fetchAccount(subscription.accountNumber, subscription.subscriptionNumber)
      invoicePreviewTargetDate = cohortSpec.earliestPriceMigrationStartDate.plusMonths(16)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId, invoicePreviewTargetDate)
      earliestStartDate <- spreadEarliestStartDate(subscription, invoicePreview, cohortSpec)
      result <- ZIO.fromEither(
        EstimationResult(account, catalogue, subscription, invoicePreview, earliestStartDate, cohortSpec)
      )
    } yield result
  }

  /*
   * Earliest start date spread out over 3 months.
   */
  def spreadEarliestStartDate(
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList,
      cohortSpec: CohortSpec
  ): IO[ConfigFailure, LocalDate] = {

    val earliestStartDate = cohortSpec.earliestPriceMigrationStartDate

    def relu(number: Int): Int =
      if (number < 0) 0 else number

    /*
      Any subscription less than one year old (i.e.: purchased within the last year), should only be risen at least one year after purchase, hence we raise them on the thirteenth month.
     */
    def startOnThirteenthMonth(earliestStartDate: LocalDate, subscription: ZuoraSubscription): UIO[Int] = {
      ZIO.succeed(
        relu(
          ChronoUnit.MONTHS.between(earliestStartDate, subscription.customerAcceptanceDate.plusMonths(13)).toInt
        )
      )
    }

    val isMonthlySubscription =
      invoicePreview.invoiceItems
        .flatMap(invoiceItem => ZuoraRatePlanCharge.matchingRatePlanCharge(subscription, invoiceItem).toOption)
        .flatMap(_.billingPeriod)
        .headOption
        .contains("Month")

    // We usually spread the start date over 3 months, but in the case of membership price rise batch 1, we want all to do through within a month
    val spreadPeriod = if (CohortSpec.isMembershipPriceRiseBatch1(cohortSpec)) 1 else 3

    if (isMonthlySubscription) {
      for {
        yearAgo <- Clock.localDateTime.map(_.toLocalDate.minusYears(1))
        randomFactor <-
          if (subscription.customerAcceptanceDate.isBefore(yearAgo)) Random.nextIntBetween(0, spreadPeriod)
          else startOnThirteenthMonth(earliestStartDate, subscription)
      } yield earliestStartDate.plusMonths(randomFactor)
    } else
      ZIO.succeed(earliestStartDate)
  }

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] =
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
