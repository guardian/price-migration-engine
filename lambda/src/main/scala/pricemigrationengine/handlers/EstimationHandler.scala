package pricemigrationengine.handlers

import java.time.{Instant, LocalDate}

import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.random.Random
import zio.{ZEnv, ZIO, ZLayer, random}

/**
  * Calculates start date and new price for a set of CohortItems.
  *
  * Expected input is a CohortSpec in json format.
  *
  * Output is a HandlerOutput in json format.
  */
object EstimationHandler extends CohortHandler {

  // TODO: move to config
  private val batchSize = 150

  def main(earliestStartDate: LocalDate): ZIO[Logging with CohortTable with Zuora with Random, Failure, HandlerOutput] =
    for {
      newProductPricing <- Zuora.fetchProductCatalogue.map(ZuoraProductCatalogue.productPricingMap)
      cohortItems <- CohortTable.fetch(ReadyForEstimation, None)
      count <-
        cohortItems
          .take(batchSize)
          .mapM(estimate(newProductPricing, earliestStartDate))
          .runCount
    } yield HandlerOutput(isComplete = count < batchSize)

  private def estimate(newProductPricing: ZuoraPricingData, earliestStartDate: LocalDate)(
      item: CohortItem
  ): ZIO[Logging with CohortTable with Zuora with Random, Failure, Unit] =
    doEstimation(newProductPricing, item, earliestStartDate).foldM(
      failure = {
        case _: AmendmentDataFailure         => CohortTable.update(CohortItem(item.subscriptionName, EstimationFailed))
        case _: CancelledSubscriptionFailure => CohortTable.update(CohortItem(item.subscriptionName, Cancelled))
        case e                               => ZIO.fail(e)
      },
      success = CohortTable.update
    )

  private def doEstimation(
      newProductPricing: ZuoraPricingData,
      item: CohortItem,
      earliestStartDate: LocalDate
  ): ZIO[Logging with CohortTable with Zuora with Random, Failure, CohortItem] =
    for {
      subscription <-
        Zuora
          .fetchSubscription(item.subscriptionName)
          .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))
      invoicePreviewTargetDate = earliestStartDate.plusMonths(13)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId, invoicePreviewTargetDate)
      earliestStartDate <- spreadEarliestStartDate(subscription, invoicePreview, earliestStartDate)
      result <-
        ZIO
          .fromEither(EstimationResult(newProductPricing, subscription, invoicePreview, earliestStartDate))
          .tapBoth(
            e =>
              Logging
                .error(s"Failed to estimate amendment data for subscription ${subscription.subscriptionNumber}: $e"),
            result => Logging.info(s"Estimated result: $result")
          )
    } yield CohortItem(
      result.subscriptionName,
      processingStage = EstimationComplete,
      oldPrice = Some(result.oldPrice),
      estimatedNewPrice = Some(result.estimatedNewPrice),
      currency = Some(result.currency),
      startDate = Some(result.startDate),
      billingPeriod = Some(result.billingPeriod),
      whenEstimationDone = Some(Instant.now())
    )

  /*
   * Earliest start date spread out over 3 months.
   */
  private[handlers] def spreadEarliestStartDate(
      subscription: ZuoraSubscription,
      invoicePreview: ZuoraInvoiceList,
      earliestStartDate: LocalDate
  ): ZIO[Random, ConfigurationFailure, LocalDate] = {

    lazy val earliestStartDateForAMonthlySub =
      for {
        randomFactor <- random.nextIntBetween(0, 3)
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

  private def env(cohortSpec: CohortSpec): ZLayer[Logging, ConfigurationFailure, CohortTable with Zuora with Logging] =
    (LiveLayer.cohortTable(cohortSpec.tableName) and LiveLayer.zuora and LiveLayer.logging)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))

  def handle(input: CohortSpec): ZIO[ZEnv with Logging, Failure, HandlerOutput] =
    main(input.earliestPriceMigrationStartDate).provideSomeLayer[ZEnv with Logging](env(input))
}
