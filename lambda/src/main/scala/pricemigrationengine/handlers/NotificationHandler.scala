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
  HomeDelivery2025Migration,
  Membership2025Migration,
  Newspaper2025P1Migration,
  Newspaper2025P3Migration,
  ProductMigration2025N4Migration
}
import pricemigrationengine.model.RatePlanProbe

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter

object NotificationHandler extends CohortHandler {

  private val batchSize = 150

  val Successful = 1
  val Unsuccessful = 0
  val Cancelled_Status = "Cancelled"

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] = {
    main(input).provideSome[Logging](
      EnvConfig.salesforce.layer,
      EnvConfig.cohortTable.layer,
      EnvConfig.emailSender.layer,
      EnvConfig.zuora.layer,
      EnvConfig.stage.layer,
      DynamoDBClientLive.impl,
      DynamoDBZIOLive.impl,
      CohortTableLive.impl(input),
      SalesforceClientLive.impl,
      EmailSenderLive.impl,
      ZuoraLive.impl
    )
  }

  def main(
      cohortSpec: CohortSpec
  ): ZIO[Logging with CohortTable with SalesforceClient with EmailSender with Zuora, Failure, HandlerOutput] = {
    for {
      today <- Clock.currentDateTime.map(_.toLocalDate)
      count <- (
        cohortSpec.subscriptionNumber match {
          case None =>
            CohortTable
              .fetch(SalesforcePriceRiseCreationComplete, Some(today.plusDays(maxLeadTime(cohortSpec))))
              .take(batchSize)
          case Some(subscriptionNumber) =>
            CohortTable
              .fetch(SalesforcePriceRiseCreationComplete, Some(today.plusDays(maxLeadTime(cohortSpec))))
              .filter(item => item.subscriptionName == subscriptionNumber)
        }
      )
        .mapZIO(item => sendNotification(cohortSpec)(item, today))
        .runCount
    } yield HandlerOutput(isComplete = count < batchSize)
  }

  private def updateCohortItemForZuoraCancellation(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[CohortTable with SalesforceClient with Logging, Failure, Unit] = {
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

  def sendNotification(cohortSpec: CohortSpec)(
      cohortItem: CohortItem,
      today: LocalDate
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Logging with Zuora, Failure, Unit] = {
    for {
      _ <- ZIO.when(!cohortSpec.forceNotifications.contains(true))(
        if (thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem)) {
          ZIO.succeed(())
        } else {
          ZIO.fail(
            NotificationNotEnoughLeadTimeFailure(
              s"[notification] The start date of item ${cohortItem.subscriptionName} (startDate: ${cohortItem.amendmentEffectiveDate}) is too close to today ${today}"
            )
          )
        }
      )
      _ <- ZIO.when(!cohortSpec.forceNotifications.contains(true))(cohortItemRatePlansChecks(cohortSpec, cohortItem))
      sfSubscription <-
        SalesforceClient
          .getSubscriptionByName(cohortItem.subscriptionName)
      // Interestingly, here we do not check whether the subscription has been
      // cancelled in Zuora, but in Salesforce.
      _ <-
        if (sfSubscription.Status__c == Cancelled_Status) {
          updateCohortItemForZuoraCancellation(cohortSpec, cohortItem)
        } else {
          sendNotification(cohortSpec, cohortItem, sfSubscription)
        }
    } yield ()
  }

  def sendNotification(
      cohortSpec: CohortSpec,
      cohortItem: CohortItem,
      sfSubscription: SalesforceSubscription
  ): ZIO[Zuora with EmailSender with SalesforceClient with CohortTable with Logging, Failure, Unit] =
    for {
      _ <- Logging.info(s"Processing subscription: ${cohortItem.subscriptionName}")
      contact <- SalesforceClient.getContact(sfSubscription.Buyer__c)
      firstName <- ZIO.fromEither(firstName(contact))
      lastName <- ZIO.fromEither(requiredField(contact.LastName, "Contact.LastName"))
      address <- ZIO.fromEither(targetAddress(cohortSpec, contact))
      street <- ZIO.fromEither(targetStreet(cohortSpec, address.street))
      postalCode = address.postalCode.getOrElse("")
      country <- ZIO.fromEither(country(cohortSpec, address))
      amendmentEffectiveDate <- ZIO.fromEither(
        requiredField(cohortItem.amendmentEffectiveDate.map(_.toString()), "CohortItem.amendmentEffectiveDate")
      )
      billingPeriod <- ZIO.fromEither(requiredField(cohortItem.billingPeriod, "CohortItem.billingPeriod"))
      paymentFrequency <- paymentFrequency(billingPeriod)
      currencyISOCode <- ZIO.fromEither(requiredField(cohortItem.currency, "CohortItem.currency"))
      currencySymbol <- currencyISOtoSymbol(currencyISOCode)
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
      // Data for HomeDelivery2025
      // This section and the corresponding section below should be removed as part of the
      // HomeDelivery2025 decommissioning.
      homedelivery2025NotificationData <- HomeDelivery2025Migration.getNotificationData(cohortSpec, cohortItem)
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

      brazeName <- brazeName(cohortSpec, cohortItem)

      message = EmailMessage(
        EmailPayload(
          Address = contact.Email,
          ContactAttributes = EmailPayloadContactAttributes(
            SubscriberAttributes = EmailPayloadSubscriberAttributes(
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
              next_payment_date = startDateConversion(amendmentEffectiveDate),
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
              // HomeDelivery2025 extension
              // This section and the corresponding section above should be removed as part of the
              // HomeDelivery decommissioning.
              homedelivery2025_brand_title = Some(homedelivery2025NotificationData.brandTitle),
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

      _ <- EmailSender.sendEmail(message)

      _ <- updateCohortItemStatus(cohortItem.subscriptionName, NotificationSendComplete)
    } yield ()

  // -------------------------------------------------------------------
  // Subscription Rate Plan Checks

  private def subscriptionRatePlansCheck(
      cohortSpec: CohortSpec,
      item: CohortItem,
      subscription: ZuoraSubscription,
      date: LocalDate
  ): ZIO[CohortTable with Zuora with SalesforceClient with Logging, Failure, Unit] = {
    for {
      _ <- RatePlanProbe.probe(subscription: ZuoraSubscription, date) match {
        case RPPShouldProceed    => ZIO.succeed(())
        case RPPCancelledInZuora =>
          updateCohortItemForZuoraCancellation(
            cohortSpec,
            item
          )
        case IndeterminateConclusion =>
          ZIO.fail(
            RatePlanProbeFailure(
              s"[4f7589ea] NotificationHandler probeRatePlans could not determine a probe outcome for cohort item ${item}. Please investigate."
            )
          )
      }
    } yield ()
  }

  private def cohortItemRatePlansChecks(
      cohortSpec: CohortSpec,
      item: CohortItem
  ): ZIO[CohortTable with Zuora with SalesforceClient with Logging, Failure, Unit] = {
    for {
      subscription <- Zuora.fetchSubscription(item.subscriptionName)
      estimationInstant <- ZIO
        .fromOption(item.whenEstimationDone)
        .mapError(ex => DataExtractionFailure(s"[3026515c] Could not extract whenEstimationDone from item ${item}"))
      _ <- subscriptionRatePlansCheck(
        cohortSpec,
        item,
        subscription,
        estimationInstant.atZone(ZoneId.of("Europe/London")).toLocalDate
      )
    } yield ()
  }

  // -------------------------------------------------------------------
  // Notification Windows

  // For general information about the notification period see the docs/notification-periods.md

  // The standard notification period for letter products (where the notification is delivered by email)
  // is -49 (included) to -35 (excluded) days. Legally the min is 30 days, but we set 35 days to alert if a
  // subscription if exiting the notification window and needs to be investigated and repaired before the deadline
  // of 30 days.

  // The digital migrations' notification window is from -33 (included) to -31 (excluded)

  def maxLeadTime(cohortSpec: CohortSpec): Int = {
    MigrationType(cohortSpec) match {
      case Test1                  => 35
      case GuardianWeekly2025     => GuardianWeekly2025Migration.maxLeadTime
      case Newspaper2025P1        => Newspaper2025P1Migration.maxLeadTime
      case HomeDelivery2025       => HomeDelivery2025Migration.maxLeadTime
      case Newspaper2025P3        => Newspaper2025P3Migration.maxLeadTime
      case ProductMigration2025N4 => ProductMigration2025N4Migration.maxLeadTime
      case Membership2025         => Membership2025Migration.maxLeadTime
      case DigiSubs2025           => DigiSubs2025Migration.maxLeadTime
    }
  }

  def minLeadTime(cohortSpec: CohortSpec): Int = {
    MigrationType(cohortSpec) match {
      case Test1                  => 33
      case GuardianWeekly2025     => GuardianWeekly2025Migration.minLeadTime
      case Newspaper2025P1        => Newspaper2025P1Migration.minLeadTime
      case HomeDelivery2025       => HomeDelivery2025Migration.minLeadTime
      case Newspaper2025P3        => Newspaper2025P3Migration.minLeadTime
      case ProductMigration2025N4 => ProductMigration2025N4Migration.minLeadTime
      case Membership2025         => Membership2025Migration.minLeadTime
      case DigiSubs2025           => DigiSubs2025Migration.minLeadTime
    }
  }

  def thereIsEnoughNotificationLeadTime(cohortSpec: CohortSpec, today: LocalDate, cohortItem: CohortItem): Boolean = {
    // To help with backward compatibility with existing tests, we apply this condition from 1st Dec 2020.
    if (today.isBefore(LocalDate.of(2020, 12, 1))) {
      true
    } else {
      cohortItem.amendmentEffectiveDate match {
        case Some(sd) => today.plusDays(minLeadTime(cohortSpec)).isBefore(sd)
        case _        => false
      }
    }
  }

  // -------------------------------------------------------------------
  // Support Functions

  def currencyISOtoSymbol(iso: String): ZIO[Any, Nothing, String] = {
    ZIO.succeed(i18n.Currency.fromString(iso: String).map(_.identifier).getOrElse(""))
  }

  def dateStrToLocalDate(startDate: String): LocalDate = {
    LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
  }

  def emailUserFriendlyDateFormatter(startDate: LocalDate): String = {
    startDate.format(DateTimeFormatter.ofPattern("d MMMM uuuu"));
  }

  def startDateConversion(startDate: String): String = {
    emailUserFriendlyDateFormatter(dateStrToLocalDate(startDate: String))
  }

  def requiredField[A](field: Option[A], fieldName: String): Either[NotificationHandlerFailure, A] = {
    field match {
      case Some(value) => Right(value)
      case None        => Left(NotificationHandlerFailure(s"$fieldName is a required field"))
    }
  }

  def targetStreet(cohortSpec: CohortSpec, street: Option[String]): Either[NotificationHandlerFailure, String] = {
    MigrationType(cohortSpec) match {
      case Membership2025 => Right(street.getOrElse(""))
      case DigiSubs2025   => Right(street.getOrElse(""))
      case _              => requiredField(street, "Contact.OtherAddress.street")
    }
  }

  def targetAddress(
      cohortSpec: CohortSpec,
      contact: SalesforceContact
  ): Either[NotificationHandlerFailure, SalesforceAddress] = {
    def testCompatibleEmptySalesforceAddress(
        contact: SalesforceContact
    ): Either[NotificationHandlerFailure, SalesforceAddress] = {
      (for {
        billingAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress")
        _ <- requiredField(billingAddress.street, "Contact.OtherAddress.street")
        _ <- requiredField(billingAddress.city, "Contact.OtherAddress.city")
      } yield billingAddress).left.flatMap(_ =>
        Right(SalesforceAddress(Some(""), Some(""), Some(""), Some(""), Some("")))
      )
    }

    MigrationType(cohortSpec) match {
      case Newspaper2025P3 => {
        // For Newspaper2025P3, we tolerate a missing delivery address and we will rely on the user getting an email.
        // For this, we compute the SalesforceAddress as the usual case, but if we get a Left,
        val address = (for {
          billingAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress")
          _ <- requiredField(billingAddress.street, "Contact.OtherAddress.street")
          _ <- requiredField(billingAddress.city, "Contact.OtherAddress.city")
        } yield billingAddress).left.flatMap(_ => requiredField(contact.MailingAddress, "Contact.MailingAddress"))
        address.fold(
          _ => Right(SalesforceAddress(Some(""), Some(""), Some(""), Some(""), Some(""))),
          value => Right(value)
        )
      }
      case ProductMigration2025N4 => {
        // We do not need the Contact.MailingAddress for ProductMigration2025N4
        // Here we prevent NotificationHandlerFailure(Contact.MailingAddress is a required field)
        val address = (for {
          billingAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress")
          _ <- requiredField(billingAddress.street, "Contact.OtherAddress.street")
          _ <- requiredField(billingAddress.city, "Contact.OtherAddress.city")
        } yield billingAddress).left.flatMap(_ => requiredField(contact.MailingAddress, "Contact.MailingAddress"))
        address.fold(
          _ => Right(SalesforceAddress(Some(""), Some(""), Some(""), Some(""), Some(""))),
          value => Right(value)
        )
      }
      case Membership2025 => {
        val address = (for {
          billingAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress")
          _ <- requiredField(billingAddress.street, "Contact.OtherAddress.street")
          _ <- requiredField(billingAddress.city, "Contact.OtherAddress.city")
        } yield billingAddress).left.flatMap(_ => requiredField(contact.MailingAddress, "Contact.MailingAddress"))
        address.fold(
          _ => Right(SalesforceAddress(Some(""), Some(""), Some(""), Some(""), Some(""))),
          value => Right(value)
        )
      }
      case DigiSubs2025 => {
        val address = (for {
          billingAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress")
          _ <- requiredField(billingAddress.street, "Contact.OtherAddress.street")
          _ <- requiredField(billingAddress.city, "Contact.OtherAddress.city")
        } yield billingAddress).left.flatMap(_ => requiredField(contact.MailingAddress, "Contact.MailingAddress"))
        address.fold(
          _ => Right(SalesforceAddress(Some(""), Some(""), Some(""), Some(""), Some(""))),
          value => Right(value)
        )
      }
      case _ =>
        (for {
          billingAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress")
          _ <- requiredField(billingAddress.street, "Contact.OtherAddress.street")
          _ <- requiredField(billingAddress.city, "Contact.OtherAddress.city")
        } yield billingAddress).left.flatMap(_ => requiredField(contact.MailingAddress, "Contact.MailingAddress"))
    }
  }

  def firstName(contact: SalesforceContact): Either[NotificationHandlerFailure, String] = {
    requiredField(contact.FirstName, "Contact.FirstName").left
      .flatMap(_ => requiredField(contact.Salutation.fold(Some("Member"))(Some(_)), "Contact.Salutation"))
  }

  def country(
      cohortSpec: CohortSpec,
      address: SalesforceAddress
  ): Either[NotificationHandlerFailure, String] = {
    // The country is usually a required field, this came from the old print migrations. It was
    // not required for the 2023 digital migrations. Although technically required for
    // the 2024 print migration, "United Kingdom" can be substituted for missing values considering
    // that we are only delivery in the UK.
    MigrationType(cohortSpec) match {
      case Newspaper2025P1        => Right(address.country.getOrElse("United Kingdom"))
      case HomeDelivery2025       => Right(address.country.getOrElse("United Kingdom"))
      case Newspaper2025P3        => Right(address.country.getOrElse("United Kingdom"))
      case ProductMigration2025N4 => Right(address.country.getOrElse(""))
      case Membership2025         => Right(address.country.getOrElse(""))
      case DigiSubs2025           => Right(address.country.getOrElse(""))
      case _                      => requiredField(address.country, "Contact.OtherAddress.country")
    }
  }

  def logMissingEmailAddress(cohortItem: CohortItem, sfContact: SalesforceContact): ZIO[Logging, Nothing, Unit] = {
    Logging
      .info(
        s"Subscription ${cohortItem.subscriptionName} is for contact ${sfContact.Id} that has not email address"
      )
      .when(sfContact.Email.isEmpty)
      .unit
  }

  private def paymentFrequency(billingPeriod: String) =
    ZIO
      .fromOption(BillingPeriod.notificationPaymentFrequencyMapping.get(billingPeriod))
      .orElseFail(EmailSenderFailure(s"No payment frequency mapping found for billing period: $billingPeriod"))

  private def updateCohortItemStatus(subscriptionNumber: String, processingStage: CohortTableFilter) = {
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
  ): ZIO[Logging with SalesforceClient, Failure, Unit] = {
    for {
      salesforcePriceRiseId <-
        ZIO
          .fromOption(cohortItem.salesforcePriceRiseId)
          .orElseFail(SalesforcePriceRiseWriteFailure("salesforcePriceRiseId is required to update Salesforce"))
      priceRise = SalesforcePriceRise(
        Migration_Name__c = Some(cohortSpec.cohortName),
        Migration_Status__c = Some("Cancellation"),
        Cancellation_Reason__c = reason
      )
      _ <- SalesforceClient.updatePriceRise(salesforcePriceRiseId, priceRise)
    } yield ()
  }

  // -------------------------------------------------------------------
  // Braze names

  // Traditionally the name of the campaign or canvas has been part of the CohortSpec, making
  // `cohortSpec.brazeName` the default carrier of this information, but for some migrations
  // we have several canvases and we want to select one depending on the subscription.

  def brazeName(cohortSpec: CohortSpec, item: CohortItem): ZIO[Zuora, Failure, String] = {
    MigrationType(cohortSpec) match {
      case ProductMigration2025N4 =>
        ZIO
          .fromOption(ProductMigration2025N4Migration.brazeName(item))
          .orElseFail(
            DataExtractionFailure(s"[0cbdf70b] could not determine brazeName for ProductMigration2025N4, item: ${item}")
          )
      case Membership2025 =>
        ZIO
          .fromOption(Membership2025Migration.brazeName(item))
          .orElseFail(
            DataExtractionFailure(s"[b9d223be] could not determine brazeName for Membership2025, item: ${item}")
          )
      case DigiSubs2025 =>
        ZIO
          .fromOption(DigiSubs2025Migration.brazeName(item))
          .orElseFail(
            DataExtractionFailure(s"[e3f83ac4] could not determine brazeName for DigiSubs2025, item: ${item}")
          )
      case _ => ZIO.succeed(cohortSpec.brazeName)
    }
  }
}
