package pricemigrationengine.handlers

import java.time.LocalDate

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
          .mapM(item =>
            estimate(newProductPricing, earliestStartDate)(item)
              .tapBoth(Logging.logFailure(item), Logging.logSuccess(item))
          )
          .runCount
          .tapError(e => Logging.error(e.toString))
    } yield HandlerOutput(isComplete = count < batchSize)

  private def estimate(newProductPricing: ZuoraPricingData, earliestStartDate: LocalDate)(
      item: CohortItem
  ): ZIO[CohortTable with Zuora with Random, Failure, EstimationResult] =
    doEstimation(newProductPricing, item, earliestStartDate).foldM(
      failure = {
        case _: AmendmentDataFailure =>
          val result = FailedEstimationResult(item.subscriptionName)
          CohortTable.update(CohortItem.fromFailedEstimationResult(result)) zipRight ZIO.succeed(result)
        case _: CancelledSubscriptionFailure =>
          val result = CancelledEstimationResult(item.subscriptionName)
          CohortTable.update(CohortItem.fromCancelledEstimationResult(result)) zipRight ZIO.succeed(result)
        case e => ZIO.fail(e)
      },
      success = { result =>
        CohortTable.update(CohortItem.fromSuccessfulEstimationResult(result)) zipRight ZIO.succeed(result)
      }
    )

  private def doEstimation(
      newProductPricing: ZuoraPricingData,
      item: CohortItem,
      earliestStartDate: LocalDate
  ): ZIO[Zuora with Random, Failure, SuccessfulEstimationResult] =
    for {
      subscription <-
        Zuora
          .fetchSubscription(item.subscriptionName)
          .filterOrFail(_.status != "Cancelled")(CancelledSubscriptionFailure(item.subscriptionName))
      invoicePreviewTargetDate = earliestStartDate.plusMonths(13)
      invoicePreview <- Zuora.fetchInvoicePreview(subscription.accountId, invoicePreviewTargetDate)
      earliestStartDate <- spreadEarliestStartDate(subscription, invoicePreview, earliestStartDate)
      result <- ZIO.fromEither(EstimationResult(newProductPricing, subscription, invoicePreview, earliestStartDate))
    } yield result

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
    (LiveLayer.cohortTable(cohortSpec) and LiveLayer.zuora and LiveLayer.logging)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))

  def handle(input: CohortSpec): ZIO[ZEnv with Logging, Failure, HandlerOutput] =
    main(input.earliestPriceMigrationStartDate).provideSomeLayer[ZEnv with Logging](env(input))
}
