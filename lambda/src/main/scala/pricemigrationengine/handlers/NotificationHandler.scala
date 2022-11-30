package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow._
import pricemigrationengine.services._
import zio.{Clock, ZIO}
import com.gu.i18n

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object NotificationHandler extends CohortHandler {

  val Successful = 1
  val Unsuccessful = 0
  val Cancelled_Status = "Cancelled"

  // We are starting the notification process for any item whose start date is less than 49 (StandardNotificationLeadTime)
  // days away, but because it can happen, we also need to react to items which for *any* reason have
  // not been processed on time (meaning early enough) and for which the previously computed startDate is
  // too close (less than MinNotificationLeadTime away) from now and would not give enough time to the letters.

  private val StandardNotificationLeadTime = 49
  private val MinNotificationLeadTime = 35

  def main(
      brazeCampaignName: String
  ): ZIO[Logging with CohortTable with SalesforceClient with EmailSender, Failure, HandlerOutput] = {
    for {
      today <- Clock.currentDateTime.map(_.toLocalDate)
      count <- CohortTable
        .fetch(SalesforcePriceRiceCreationComplete, Some(today.plusDays(StandardNotificationLeadTime)))
        .mapZIO(sendNotification(brazeCampaignName))
        .runFold(0) { (sum, count) => sum + count }
      _ <- Logging.info(s"Successfully sent $count price rise notifications")
    } yield HandlerOutput(isComplete = true)
  }

  def itemHasEnoughNotificationPadding(today: LocalDate, cohortItem: CohortItem): Boolean = {
    // To help with backward compatibility with existing tests, we apply this condition from 1st Dec 2022.
    if (today.isBefore(LocalDate.of(2022, 12, 1))) {
      true
    } else {
      cohortItem.startDate match {
        case Some(sd) => today.plusDays(MinNotificationLeadTime).isBefore(sd)
        case _        => false
      }
    }
  }

  def sendNotification(brazeCampaignName: String)(
      cohortItem: CohortItem
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Logging, Failure, Int] = {

    // We are starting with a simple check. That the item's startDate is at least MinNotificationLeadTime days away
    // from the current day. This will avoid headaches caused by letters not being sent early enough relatively to
    // previously computed start dats, and will detect any such problem when they happen and that
    // before the letters are sent.

    if (itemHasEnoughNotificationPadding(LocalDate.now(), cohortItem)) {

      val result = for {
        sfSubscription <-
          SalesforceClient
            .getSubscriptionByName(cohortItem.subscriptionName)
        count <-
          if (sfSubscription.Status__c != Cancelled_Status) {
            sendNotification(brazeCampaignName, cohortItem, sfSubscription)
          } else {
            putSubIntoCancelledStatus(cohortItem.subscriptionName)
          }
      } yield count

      result.catchAll { failure =>
        for {
          _ <- Logging.error(
            s"Subscription ${cohortItem.subscriptionName}: Failed to send price rise notification: $failure"
          )
        } yield Unsuccessful
      }

    } else {

      ZIO.fail(
        NotificationNotEnoughLeadTimeFailure(
          s"The start date of item ${cohortItem.subscriptionName} (startDate: ${cohortItem.startDate}) is too close to today ${LocalDate.now()}"
        )
      )

    }
  }

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

  def sendNotification(
      brazeCampaignName: String,
      cohortItem: CohortItem,
      sfSubscription: SalesforceSubscription
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Logging, Failure, Int] =
    for {
      _ <- Logging.info(s"Processing subscription: ${cohortItem.subscriptionName}")
      contact <- SalesforceClient.getContact(sfSubscription.Buyer__c)
      firstName <- requiredField(contact.FirstName, "Contact.FirstName").orElse(
        requiredField(contact.Salutation, "Contact.Salutation")
      )
      lastName <- requiredField(contact.LastName, "Contact.LastName")
      address <- targetAddress(contact)
      street <- requiredField(address.street, "Contact.OtherAddress.street")
      postalCode <- requiredField(address.postalCode, "Contact.OtherAddress.postalCode")
      country <- requiredField(address.country, "Contact.OtherAddress.country")
      estimatedNewPrice <- requiredField(cohortItem.estimatedNewPrice.map(_.toString()), "CohortItem.estimatedNewPrice")
      startDate <- requiredField(cohortItem.startDate.map(_.toString()), "CohortItem.startDate")
      billingPeriod <- requiredField(cohortItem.billingPeriod, "CohortItem.billingPeriod")
      paymentFrequency <- paymentFrequency(billingPeriod)
      currencyISOCode <- requiredField(cohortItem.currency, "CohortItem.currency")
      currencySymbol <- currencyISOtoSymbol(currencyISOCode)
      estimatedNewPriceWithCurrencySymbol = s"${currencySymbol}${estimatedNewPrice}"

      _ <- logMissingEmailAddress(cohortItem, contact)

      _ <- updateCohortItemStatus(cohortItem.subscriptionName, NotificationSendProcessingOrError)

      _ <- EmailSender.sendEmail(
        message = EmailMessage(
          EmailPayload(
            Address = contact.Email,
            ContactAttributes = EmailPayloadContactAttributes(
              SubscriberAttributes = EmailPayloadSubscriberAttributes(
                title =
                  contact.FirstName flatMap (_ =>
                    contact.Salutation
                  ), // if no first name, we use salutation as first name and leave this field empty
                first_name = firstName,
                last_name = lastName,
                billing_address_1 = street,
                billing_address_2 = None, // See 'Billing Address Format' section in the readme
                billing_city = address.city,
                billing_postal_code = postalCode,
                billing_state = address.state,
                billing_country = country,
                payment_amount = estimatedNewPriceWithCurrencySymbol,
                next_payment_date = startDateConversion(startDate),
                payment_frequency = paymentFrequency,
                subscription_id = cohortItem.subscriptionName,
                product_type = sfSubscription.Product_Type__c.getOrElse("")
              )
            )
          ),
          brazeCampaignName,
          contact.Id,
          contact.IdentityID__c
        )
      )

      _ <- updateCohortItemStatus(cohortItem.subscriptionName, NotificationSendComplete)
    } yield Successful

  def targetAddress(contact: SalesforceContact): ZIO[Any, NotificationHandlerFailure, SalesforceAddress] =
    (for {
      billingAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress")
      _ <- requiredField(billingAddress.street, "Contact.OtherAddress.street")
      _ <- requiredField(billingAddress.city, "Contact.OtherAddress.city")
    } yield billingAddress).orElse(
      requiredField(contact.MailingAddress, "Contact.MailingAddress")
    )

  def requiredField[A](field: Option[A], fieldName: String): ZIO[Any, NotificationHandlerFailure, A] = {
    ZIO.fromOption(field).orElseFail(NotificationHandlerFailure(s"$fieldName is a required field"))
  }

  def logMissingEmailAddress(cohortItem: CohortItem, sfContact: SalesforceContact): ZIO[Logging, Nothing, Unit] = {
    Logging
      .info(
        s"Subscription ${cohortItem.subscriptionName} is for contact ${sfContact.Id} that has not email address"
      )
      .when(sfContact.Email.isEmpty)
      .unit
  }

  val paymentFrequencyMapping = Map(
    "Month" -> "Monthly",
    "Quarter" -> "Quarterly",
    "Semi_Annual" -> "Semiannually",
    "Annual" -> "Annually"
  )

  private def paymentFrequency(billingPeriod: String) =
    ZIO
      .fromOption(paymentFrequencyMapping.get(billingPeriod))
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

  def putSubIntoCancelledStatus(subscriptionName: String): ZIO[Logging with CohortTable, Failure, Int] =
    for {
      _ <-
        CohortTable
          .update(
            CohortItem(
              subscriptionName = subscriptionName,
              processingStage = Cancelled
            )
          )
      _ <- Logging.error(s"Subscription $subscriptionName has been cancelled, price rise notification not sent")
    } yield 0

  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput] =
    main(input.brazeCampaignName).provideSome[Logging](
      EnvConfig.salesforce.layer,
      EnvConfig.cohortTable.layer,
      EnvConfig.emailSender.layer,
      EnvConfig.stage.layer,
      DynamoDBClientLive.impl,
      DynamoDBZIOLive.impl,
      CohortTableLive.impl(input),
      SalesforceClientLive.impl,
      EmailSenderLive.impl
    )
}
