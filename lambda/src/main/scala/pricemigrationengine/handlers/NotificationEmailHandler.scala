package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.Context
import pricemigrationengine.model.CohortTableFilter.AmendmentComplete
import pricemigrationengine.model.membershipworkflow.{EmailMessage, EmailPayload, EmailPayloadContactAttributes}
import pricemigrationengine.model.{CohortItem, Failure, NotificationEmailFailure}
import pricemigrationengine.services._
import zio.clock.Clock
import zio.console.Console
import zio.{Runtime, ZEnv, ZIO, ZLayer, clock, console}

object NotificationEmailHandler {
  private val NotificationEmailLeadTimeDays = 30

  val main: ZIO[Logging with CohortTable with SalesforceClient with Clock with EmailSender, Failure, Unit] = {
    for {
      now <- clock.currentDateTime.mapError(ex => NotificationEmailFailure(s"Failed to get time: $ex"))
      subscriptions <- CohortTable.fetch(
        AmendmentComplete, Some(now.toLocalDate.plusDays(NotificationEmailLeadTimeDays))
      )
      _ <- subscriptions.foreach(sendEmail)
    } yield ()
  }

  def sendEmail(cohortItem: CohortItem): ZIO[EmailSender with SalesforceClient, Failure, Unit] = {
    for {
      sfSubscription <- SalesforceClient.getSubscriptionByName(cohortItem.subscriptionName)
      contact <- SalesforceClient.getContact(sfSubscription.Recipient__c)
      emailAddress <- requiredField(contact.Email, "Contact.Email")
      firstName <- requiredField(contact.FirstName, "Contact.FirstName")
      lastName <- requiredField(contact.LastName, "Contact.LastName")
      street <- requiredField(contact.MailingAddress.street, "Contact.MailingAddress.street")
      city <- requiredField(contact.MailingAddress.city, "Contact.MailingAddress.city")
      postalCode <- requiredField(contact.MailingAddress.postalCode, "Contact.MailingAddress.postalCode")
      state <- requiredField(contact.MailingAddress.state, "Contact.MailingAddress.state")
      country <- requiredField(contact.MailingAddress.country, "Contact.MailingAddress.country")
      newPrice <- requiredField(cohortItem.newPrice.map(_.toString()), "CohortItem.newPrice")
      startDate <- requiredField(cohortItem.startDate.map(_.toString()), "CohortItem.startDate")
      billingPeriod <- requiredField(cohortItem.billingPeriod, "CohortItem.billingPeriod")

      _ <- EmailSender.sendEmail(
        message = EmailMessage(
          EmailPayload(
            Address = emailAddress,
            ContactAttributes = EmailPayloadContactAttributes(
              FirstName = firstName,
              LastName = lastName,
              AddressLine1 = street,
              Town = city,
              Postcode = postalCode,
              County = state,
              Country = country,
              NewPrice = newPrice,
              StartDate = startDate,
              BillingPeriod = billingPeriod
            )
          ),
          "price-rise-email",
          contact.Id,
          contact.IdentityID__c
        )
      )
    } yield ()
  }

  def requiredField(field: Option[String], fieldName: String) = {
    ZIO.fromOption(field).orElseFail(NotificationEmailFailure(s"$fieldName is a required field"))
  }


  private def env(
    loggingLayer: ZLayer[Any, Nothing, Logging]
  ): ZLayer[Any, Any, Logging with CohortTable with SalesforceClient with Clock with EmailSender] = {
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++
          EnvConfiguration.stageImp ++ EnvConfiguration.salesforceImp >>>
        CohortTableLive.impl ++ SalesforceClientLive.impl ++ Clock.live ++ EmailSenderLive.impl
    loggingLayer ++ cohortTableLayer
  }

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideSomeLayer(
        env(Console.live >>> ConsoleLogging.impl)
      )
      .foldM(
        e => console.putStrLn(s"Failed: $e") *> ZIO.succeed(1),
        _ => console.putStrLn("Succeeded!") *> ZIO.succeed(0)
      )

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.impl(context))
      )
    )
}
