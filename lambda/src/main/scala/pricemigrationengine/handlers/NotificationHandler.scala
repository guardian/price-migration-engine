package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow._
import pricemigrationengine.services._
import zio.{Clock, ZIO}
import com.gu.i18n
import pricemigrationengine.migrations.{
  DigiSubs2025Migration,
  GuardianWeekly2025Migration,
  Membership2025Migration,
  Newspaper2025P1Migration,
  Newspaper2025P3Migration,
  Newspaper2026MigrationX,
  ProductMigration2025N4Migration,
  SupporterPlus2026Migration
}
import pricemigrationengine.model.RatePlanProbe

import java.time.{LocalDate, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter

object NotificationHandler extends CohortHandler {

  private val batchSize = 150

  // -----------------------------------------
  // Primary Logic
  // -----------------------------------------

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    main(input).provideSome[Logging](
      EnvConfig.salesforce.layer,
      EnvConfig.braze.layer,
      EnvConfig.zuora.layer,
      EnvConfig.stage.layer,
      DynamoDBLive.impl,
      DynamoDBZIOLive.impl,
      CohortTableLive.impl(input),
      SalesforceLive.impl,
      BrazeLive.impl,
      ZuoraLive.impl
    )
  }

  def main(
      cohortSpec: CohortSpec
  ): ZIO[Logging with CohortTable with Salesforce with Braze with Zuora, Failure, HandlerOutput] = {
    for {
      today <- Clock.currentDateTime.map(_.toLocalDate)
      count <- (
        cohortSpec.subscriptionNumber match {
          case None =>
            CohortTable
              .fetch(
                SalesforcePriceRiseCreationComplete,
                Some(today.plusDays(NotificationHandlerHelper.notificationLeadTime(cohortSpec)))
              )
              .take(batchSize)
          case Some(subscriptionNumber) =>
            CohortTable
              .fetch(
                SalesforcePriceRiseCreationComplete,
                Some(today.plusDays(NotificationHandlerHelper.notificationLeadTime(cohortSpec)))
              )
              .filter(item => item.subscriptionName == subscriptionNumber)
        }
      ).mapZIO { item => processCohortItem(cohortSpec, item, today) }.runCount
    } yield HandlerOutput(isComplete = count < batchSize)
  }

  def processCohortItem(
      cohortSpec: CohortSpec,
      item: CohortItem,
      today: LocalDate
  ): ZIO[CohortTable with Salesforce with Logging with Braze with Zuora, Failure, Unit] = {
    for {
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      estimationInstant <- ZIO
        .fromOption(item.whenEstimationDone)
        .mapError(ex => DataExtractionFailure(s"[3026515c] Could not extract whenEstimationDone from item ${item}"))
      ratePlanProbeResult <- ZIO.succeed(
        RatePlanProbe.probe(subscription: ZuoraSubscription, LocalDate.ofInstant(estimationInstant, ZoneOffset.UTC))
      )
      analyseResult <- ZIO
        .fromOption(
          SubscriptionNotificationAnalyseResult.analyseSubscriptionForNotification(
            cohortSpec,
            subscription,
            item,
            today,
            ratePlanProbeResult
          )
        )
        .orElseFail(
          DataExtractionFailure(
            s"[0c1a6fc5] could not determine SubscriptionNotificationAnalyseResult for item {$item}"
          )
        )
      _ <- Logging.info(
        s"[dc6a8cb4] analyse subscription for notification, item: ${item}, result: ${SubscriptionNotificationAnalyseResult.toString(analyseResult)}"
      )
      _ <- evaluateAnalyseResult(cohortSpec, item, subscription, analyseResult, today)
    } yield ()
  }

  def evaluateAnalyseResult(
      cohortSpec: CohortSpec,
      item: CohortItem,
      zuoraSubscription: ZuoraSubscription,
      analyseResult: SubscriptionNotificationAnalyseResult,
      today: LocalDate
  ): ZIO[CohortTable with Salesforce with Logging with Braze with Zuora, Failure, Unit] = {
    analyseResult match {
      case SNARReadyToNotify             => sendNotification(cohortSpec, zuoraSubscription, item, today)
      case SNARCancelledInZuora          => updateCohortItemToReflectZuoraCancellation(cohortSpec, item)
      case SNARExcludeFromMigration      => updateCohortItemToExcludeFromMigration(item)
      case SNARMissingNotificationWindow =>
        ZIO.fail(
          NotificationHandlerFailure(
            s"[71edb83e] we are missing the notification window for ${item} (SubscriptionNotificationAnalyseResult). Please investigate."
          )
        )
    }
  }

  // -----------------------------------------
  // Helpers
  // -----------------------------------------

  def requiredField[A](field: Option[A], fieldName: String): Either[NotificationHandlerFailure, A] = {
    field match {
      case Some(value) => Right(value)
      case None        => Left(NotificationHandlerFailure(s"$fieldName is a required field"))
    }
  }

  private def updateCohortItemToExcludeFromMigration(
      item: CohortItem
  ): ZIO[CohortTable with Salesforce with Logging, Failure, Unit] = {
    for {
      _ <- CohortTable
        .update(
          CohortItem(
            item.subscriptionName,
            processingStage = ExcludedFromMigration,
            cancellationReason =
              Some("(cause: fae335fc) excluded from migration by SubscriptionNotificationAnalyseResult")
          )
        )
      _ <- Logging.info(
        s"Subscription ${item.subscriptionName} has been excluded from migration by SubscriptionNotificationAnalyseResult"
      )
    } yield ()
  }

  private def updateCohortItemToReflectZuoraCancellation(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[CohortTable with Salesforce with Logging, Failure, Unit] = {
    for {
      _ <- CohortTable
        .update(
          CohortItem(
            item.subscriptionName,
            processingStage = ZuoraCancellation,
            cancellationReason = Some("(cause: 91a2874c) Subscription has been cancelled in Zuora")
          )
        )
      _ <- notifySalesforceOfCancelledStatus(cohortSpec, item, Some("Subscription has been cancelled in Zuora"))
      _ <- Logging.info(
        s"Subscription ${item.subscriptionName} has been cancelled in Zuora, price rise notification not sent"
      )
    } yield ()
  }

  def sendNotification(
      cohortSpec: CohortSpec,
      zuoraSubscription: ZuoraSubscription,
      cohortItem: CohortItem,
      today: LocalDate
  ): ZIO[Zuora with Braze with Salesforce with CohortTable with Logging, Failure, Unit] =
    for {
      _ <- Logging.info(s"Processing subscription: ${cohortItem.subscriptionName}")
      sfSubscription <-
        Salesforce
          .getSubscriptionByName(cohortItem.subscriptionName)
      contact <- Salesforce.getContact(sfSubscription.Buyer__c)
      firstName <- ZIO.fromEither(NotificationHandlerHelper.firstName(contact))
      lastName <- ZIO.fromEither(requiredField(contact.LastName, "Contact.LastName"))
      address <- ZIO.fromEither(NotificationHandlerHelper.targetAddress(cohortSpec, contact))
      street <- ZIO.fromEither(NotificationHandlerHelper.targetStreet(cohortSpec, address.street))
      postalCode = address.postalCode.getOrElse("")
      country <- ZIO.fromEither(NotificationHandlerHelper.country(cohortSpec, address))
      amendmentEffectiveDate <- ZIO.fromEither(
        requiredField(cohortItem.amendmentEffectiveDate.map(_.toString()), "CohortItem.amendmentEffectiveDate")
      )
      billingPeriod <- ZIO.fromEither(requiredField(cohortItem.billingPeriod, "CohortItem.billingPeriod"))
      paymentFrequency <- ZIO
        .fromOption(BillingPeriod.notificationPaymentFrequencyMapping.get(billingPeriod))
        .orElseFail(BrazeFailure(s"No payment frequency mapping found for billing period: $billingPeriod"))
      currencyISOCode <- ZIO.fromEither(requiredField(cohortItem.currency, "CohortItem.currency"))
      currencySymbol <- ZIO.succeed(i18n.Currency.fromString(currencyISOCode).map(_.identifier).getOrElse(""))
      commsPrice <- ZIO
        .fromOption(cohortItem.commsPrice)
        .orElseFail(DataExtractionFailure(s"[cd945387] $cohortItem does not have a commsPrice"))
      commsPriceWithCurrencySymbol = s"${currencySymbol}${commsPrice}"

      _ <- logMissingEmailAddress(cohortItem, contact)

      // ----------------------------------------------------
      // Data for Newspaper2025P1
      // (Comment Group: 571dac68)
      // This section and the corresponding section below should be removed as part of the
      // Newspaper2025P1 decommissioning.
      newspaper2025P1NotificationData <- Newspaper2025P1Migration.getNotificationData(cohortSpec, cohortItem)
      // ----------------------------------------------------

      // ----------------------------------------------------
      // Data for Newspaper2025P3
      newspaper2025P3NotificationData <- Newspaper2025P3Migration.getNotificationData(cohortSpec, cohortItem)
      // ----------------------------------------------------

      // ----------------------------------------------------
      // Data for ProductMigration2025N4
      productMigration2025N4NotificationData <-
        ZIO
          .fromOption(
            ProductMigration2025N4Migration.getNotificationData(
              cohortSpec,
              cohortItem
            )
          )
          .orElseFail(DataExtractionFailure(s"[c20f44b1] How did we get here ? 🤔"))
      // ----------------------------------------------------

      // ----------------------------------------------------
      // Data for SupporterPlus2026
      supporterPlus2026ExtraData <-
        ZIO
          .fromOption(
            SupporterPlus2026Migration.extractEmailExtraAttributes(
              cohortSpec,
              cohortItem,
              zuoraSubscription,
            )
          )
          .orElseFail(DataExtractionFailure(s"[2ae40ea0] How did we get here ? 🤔"))
      // ----------------------------------------------------

      // ----------------------------------------------------
      // Data for Newspaper2026X
      newspaper2026_brand_title <- ZIO
        .fromOption(
          Newspaper2026MigrationX.decideBranchTitleForNotificationHandler(cohortSpec, zuoraSubscription, today)
        )
        .orElseFail(DataExtractionFailure(s"[47a5291e] How did we get here ? 🤔"))
      // ----------------------------------------------------

      brazeName <- ZIO
        .fromOption(NotificationHandlerHelper.decideBrazeName(cohortSpec, cohortItem, zuoraSubscription))
        .orElseFail(
          DataExtractionFailure(
            s"[af851468] could not determine brazeName for ${cohortSpec.cohortName}, item: ${cohortItem.subscriptionName}"
          )
        )

      message = BrazeMessage(
        BrazePayload(
          Address = contact.Email,
          ContactAttributes = BrazePayloadContactAttributes(
            SubscriberAttributes = BrazePayloadSubscriberAttributes(
              title = contact.FirstName flatMap (_ =>
                contact.Salutation // if no first name, we use salutation as first name and leave this field empty
              ),
              first_name = firstName,
              last_name = lastName,
              billing_address_1 = street,
              billing_address_2 = None, // See 'Billing Address Format' section in the readme
              billing_city = address.city,
              billing_postal_code = postalCode,
              billing_state = address.state,
              billing_country = country,
              payment_amount = commsPriceWithCurrencySymbol, // [1]
              next_payment_date = NotificationHandlerHelper.startDateConversion(amendmentEffectiveDate),
              payment_frequency = paymentFrequency,
              subscription_id = cohortItem.subscriptionName,
              product_type = sfSubscription.Product_Type__c.getOrElse(""),

              // -------------------------------------------------------------
              // Newspaper2025P1 extension
              // (Comment Group: 571dac68)
              // This section and the corresponding section above should be removed as part of the
              // Newspaper2025P1 decommissioning.
              newspaper2025_brand_title = Some(newspaper2025P1NotificationData.brandTitle),
              // -------------------------------------------------------------

              // -------------------------------------------------------------
              // Newspaper2025P3 extension
              newspaper2025_phase3_brand_title = Some(newspaper2025P3NotificationData.brandTitle),
              // -------------------------------------------------------------

              // -------------------------------------------------------------
              // ProductMigration2025N4 extension
              newspaper2025_phase4_brand_title = Some(productMigration2025N4NotificationData.brandTitle),
              newspaper2025_phase4_formstack_url = Some(productMigration2025N4NotificationData.formstackUrl),
              // -------------------------------------------------------------

              // -------------------------------------------------------------
              // SupporterPlus2026 extension
              sp2026_contribution_amount = Some(s"${currencySymbol}${supporterPlus2026ExtraData.contributionAmount}"),
              sp2026_current_combined_amount =
                Some(s"${currencySymbol}${supporterPlus2026ExtraData.currentCombinedAmount}"),
              sp2026_new_combined_amount = Some(s"${currencySymbol}${supporterPlus2026ExtraData.newCombinedAmount}"),
              // -------------------------------------------------------------

              // -------------------------------------------------------------
              // Newspaper2026X
              newspaper2026_brand_title = Some(newspaper2026_brand_title)
              // -------------------------------------------------------------

            )
          )
        ),
        brazeName,
        contact.Id,
        contact.IdentityID__c
      )

      _ <- Logging.info(s"item: ${cohortItem.toString}, message: ${message.toString}")

      _ <- ZIO.when(!NotificationHandlerHelper.messageIsWellFormed(cohortSpec, message))(
        ZIO.fail(NotificationHandlerFailure(s"item: ${cohortItem.toString} has failed email integrity check"))
      )

      _ <- Braze.sendMessage(message)

      _ <- updateCohortItemStatus(cohortItem.subscriptionName, NotificationSendComplete)
    } yield ()

  // -------------------------------------------------------------------

  def logMissingEmailAddress(cohortItem: CohortItem, sfContact: SalesforceContact): ZIO[Logging, Nothing, Unit] = {
    Logging
      .info(
        s"Subscription ${cohortItem.subscriptionName} is for contact ${sfContact.Id} that has not email address"
      )
      .when(sfContact.Email.isEmpty)
      .unit
  }

  private def updateCohortItemStatus(
      subscriptionNumber: String,
      processingStage: CohortTableFilter
  ) = {
    for {
      now <- Clock.instant
      _ <-
        CohortTable
          .update(
            CohortItem(
              subscriptionName = subscriptionNumber,
              processingStage = processingStage,
              whenNotificationSent = Some(now)
            )
          )
          .mapError { error =>
            NotificationHandlerFailure(s"Failed set status CohortItem $subscriptionNumber to $processingStage: $error")
          }
    } yield ()
  }

  def notifySalesforceOfCancelledStatus(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      reason: Option[String]
  ): ZIO[Logging with Salesforce, Failure, Unit] = {
    for {
      salesforcePriceRiseId <-
        ZIO
          .fromOption(cohortItem.salesforcePriceRiseId)
          .orElseFail(
            SalesforcePriceRiseWriteFailure(
              s"[e8e1426c] salesforcePriceRiseId is required to update Salesforce (cohort item: ${cohortItem.subscriptionName})"
            )
          )
      priceRise = SalesforcePriceRise(
        Migration_Name__c = Some(cohortSpec.cohortName),
        Migration_Status__c = Some("Cancellation"),
        Cancellation_Reason__c = reason
      )
      _ <- Salesforce.updatePriceRise(salesforcePriceRiseId, priceRise)
    } yield ()
  }
}
