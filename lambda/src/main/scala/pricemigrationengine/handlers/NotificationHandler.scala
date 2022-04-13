package pricemigrationengine.handlers

import pricemigrationengine.model.CohortTableFilter.{
  Cancelled,
  NotificationSendComplete,
  NotificationSendProcessingOrError,
  SalesforcePriceRiceCreationComplete
}
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow.{
  EmailMessage,
  EmailPayload,
  EmailPayloadContactAttributes,
  EmailPayloadSubscriberAttributes
}
import pricemigrationengine.services._
import zio.Clock
import zio.{ZEnv, ZIO, ZLayer}

object NotificationHandler extends CohortHandler {

  val Successful = 1
  val Unsuccessful = 0

  val Cancelled_Status = "Cancelled"

  private val NotificationLeadTimeDays = 37

  def main(
      brazeCampaignName: String
  ): ZIO[Logging with CohortTable with SalesforceClient with Clock with EmailSender, Failure, HandlerOutput] = {
    for {
      today <- Time.today
      subscriptions <- CohortTable.fetch(
        SalesforcePriceRiceCreationComplete,
        Some(today.plusDays(NotificationLeadTimeDays))
      )
      count <-
        subscriptions
          .mapZIO(sendNotification(brazeCampaignName))
          .runFold(0) { (sum, count) => sum + count }
      _ <- Logging.info(s"Successfully sent $count price rise notifications")
    } yield HandlerOutput(isComplete = true)
  }

  def sendNotification(brazeCampaignName: String)(
      cohortItem: CohortItem
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Clock with Logging, Failure, Int] = {
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
  }

  def sendNotification(
      brazeCampaignName: String,
      cohortItem: CohortItem,
      sfSubscription: SalesforceSubscription
  ): ZIO[EmailSender with SalesforceClient with CohortTable with Clock with Logging, Failure, Int] =
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
                payment_amount = estimatedNewPrice,
                next_payment_date = startDate,
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
      now <- Time.thisInstant
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

  private def env(
      cohortSpec: CohortSpec
  ): ZLayer[Logging, Failure, CohortTable with SalesforceClient with EmailSender with Logging] =
    (LiveLayer.cohortTable(cohortSpec) and LiveLayer.salesforce and LiveLayer.emailSender and LiveLayer.logging)
      .tapError(e => Logging.error(s"Failed to create service environment: $e"))

  def handle(input: CohortSpec): ZIO[ZEnv with Logging, Failure, HandlerOutput] =
    main(input.brazeCampaignName).provideSomeLayer[ZEnv with Logging](env(input))
}
