package pricemigrationengine.handlers

import pricemigrationengine.TestLogging
import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow.EmailMessage
import pricemigrationengine.services._
import pricemigrationengine.util.Runner.unsafeRunSync
import zio.Exit.Success
import zio.Runtime.default
import zio._
import zio.stream.ZStream
import zio.test.{TestClock, testEnvironment}

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneOffset}
import scala.collection.mutable.ArrayBuffer

class NotificationHandlerTest extends munit.FunSuite {
  private val expectedSubscriptionName = "Sub-0001"
  private val expectedStartDate = LocalDate.of(2020, 1, 1)
  private val expectedCurrency = "GBP"
  private val expectedBillingPeriod = "Month"
  private val expectedBillingPeriodInNotification = "Monthly"
  private val expectedOldPrice = BigDecimal(11.11)
  private val expectedEstimatedNewPrice = BigDecimal(22.22)
  private val expectedEstimatedNewPriceWithCurrencySymbolPrefix = "£22.22"
  private val expectedSFSubscriptionId = "1234"
  private val expectedBuyerId = "buyer-1"
  private val expectedIdentityId = "buyer1-identity-id"
  private val expectedEmailAddress = "buyer@email.address"
  private val expectedFirstName = "buyer1FirstName"
  private val expectedLastName = "buyer1LastName"
  private val expectedStreet = "buyer1Street"
  private val expectedCity = "buyer1City"
  private val expectedState = "buyer1State"
  private val expectedPostalCode = "buyer1PostalCode"
  private val expectedCountry = "buyer1Country"
  private val expectedDataExtensionName = "SV_VO_Pricerise_Q22020"
  private val expectedSalutation = "Ms"
  private val expectedSfStatus = "Active"
  private val expectedProductType = "Newspaper - Digital Voucher"
  private val expectedCurrentTime = Instant.parse("2020-05-21T15:16:37Z")

  private val mailingAddressStreet = "buyer1MailStreet"
  private val mailingAddressCity = "buyer1MailCity"
  private val mailingAddressState = "buyer1MailState"
  private val mailingAddressPostalCode = "buyer1MailPostalCode"
  private val mailingAddressCountry = "buyer1MailCountry"

  private val brazeCampaignName = "SV_VO_Pricerise_Q22020"

  private def createStubCohortTable(
      updatedResultsWrittenToCohortTable: ArrayBuffer[CohortItem],
      cohortItem: CohortItem
  ) = {
    ZLayer.succeed(
      new CohortTable {

        override def fetch(
            filter: CohortTableFilter,
            beforeDateInclusive: Option[LocalDate]
        ): ZStream[Any, CohortFetchFailure, CohortItem] = {
          assertEquals(filter, SalesforcePriceRiceCreationComplete)
          assertEquals(
            beforeDateInclusive,
            Some(
              LocalDate
                .from(expectedCurrentTime.plus(49, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC))
            )
          )
          ZStream(cohortItem)
        }

        override def create(cohortItem: CohortItem): ZIO[Any, Failure, Unit] = ???

        override def update(result: CohortItem): ZIO[Any, CohortUpdateFailure, Unit] = {
          updatedResultsWrittenToCohortTable.addOne(result)
          ZIO.succeed(())
        }

        override def fetchAll(): ZStream[Any, CohortFetchFailure, CohortItem] = ???
      }
    )
  }

  private def stubSFClient(
      subscriptions: List[SalesforceSubscription],
      contacts: List[SalesforceContact]
  ) = {
    ZLayer.succeed(
      new SalesforceClient {

        override def getSubscriptionByName(
            subscriptionName: String
        ): IO[SalesforceClientFailure, SalesforceSubscription] =
          ZIO
            .fromOption(subscriptions.find(_.Name == subscriptionName))
            .orElseFail(SalesforceClientFailure(s"No subscription for name '$subscriptionName'"))

        override def createPriceRise(
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, SalesforcePriceRiseCreationResponse] = ???

        override def updatePriceRise(
            priceRiseId: String,
            priceRise: SalesforcePriceRise
        ): IO[SalesforceClientFailure, Unit] = ???

        override def getContact(
            contactId: String
        ): IO[SalesforceClientFailure, SalesforceContact] =
          ZIO
            .fromOption(contacts.find(_.Id == contactId))
            .orElseFail(SalesforceClientFailure(s"No subscription for name '$contactId'"))
      }
    )
  }

  private def createStubEmailSender(sendMessages: ArrayBuffer[EmailMessage]) = {
    ZLayer.succeed(
      new EmailSender {
        override def sendEmail(message: EmailMessage): ZIO[Any, EmailSenderFailure, Unit] =
          ZIO
            .attempt {
              sendMessages.addOne(message)
              ()
            }
            .orElseFail(EmailSenderFailure(""))
      }
    )
  }

  private def createFailingStubEmailSender() = {
    ZLayer.succeed(
      new EmailSender {
        override def sendEmail(message: EmailMessage): ZIO[Any, EmailSenderFailure, Unit] =
          ZIO.fail(EmailSenderFailure("Bang!!"))
      }
    )
  }

  private val salesforceSubscription: SalesforceSubscription =
    SalesforceSubscription(
      expectedSFSubscriptionId,
      expectedSubscriptionName,
      expectedBuyerId,
      expectedSfStatus,
      Some(expectedProductType)
    )

  private val salesforceContact: SalesforceContact =
    SalesforceContact(
      Id = expectedBuyerId,
      IdentityID__c = Some(expectedIdentityId),
      Email = Some(expectedEmailAddress),
      Salutation = Some(expectedSalutation),
      FirstName = Some(expectedFirstName),
      LastName = Some(expectedLastName),
      OtherAddress = Some(
        SalesforceAddress(
          street = Some(expectedStreet),
          city = Some(expectedCity),
          state = Some(expectedState),
          postalCode = Some(expectedPostalCode),
          country = Some(expectedCountry)
        )
      ),
      MailingAddress = Some(
        SalesforceAddress(
          street = Some(mailingAddressStreet),
          city = Some(mailingAddressCity),
          state = Some(mailingAddressState),
          postalCode = Some(mailingAddressPostalCode),
          country = Some(mailingAddressCountry)
        )
      )
    )

  private val cohortItem =
    CohortItem(
      subscriptionName = expectedSubscriptionName,
      processingStage = AmendmentComplete,
      startDate = Some(expectedStartDate),
      currency = Some(expectedCurrency),
      oldPrice = Some(expectedOldPrice),
      estimatedNewPrice = Some(expectedEstimatedNewPrice),
      billingPeriod = Some(expectedBillingPeriod)
    )

  test("NotificationHandler should get records from cohort table and SF and send Email with the data") {
    val stubSalesforceClient = stubSFClient(List(salesforceSubscription), List(salesforceContact))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val sentMessages = ArrayBuffer[EmailMessage]()
    val stubEmailSender = createStubEmailSender(sentMessages)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(brazeCampaignName)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(sentMessages.size, 1)
    assertEquals(sentMessages(0).DataExtensionName, expectedDataExtensionName)
    assertEquals(sentMessages(0).SfContactId, expectedBuyerId)
    assertEquals(sentMessages(0).IdentityUserId, Some(expectedIdentityId))
    assertEquals(sentMessages(0).To.Address, Some(expectedEmailAddress))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_1, expectedStreet)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_2, None)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_city, Some(expectedCity))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_state, Some(expectedState))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_postal_code, expectedPostalCode)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_country, expectedCountry)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.title, Some(expectedSalutation))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.first_name, expectedFirstName)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.last_name, expectedLastName)
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.payment_amount,
      expectedEstimatedNewPriceWithCurrencySymbolPrefix
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.next_payment_date,
      expectedStartDate.toString
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.payment_frequency,
      expectedBillingPeriodInNotification
    )
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.subscription_id, expectedSubscriptionName)

    assertEquals(updatedResultsWrittenToCohortTable.size, 2)
    assertEquals(
      updatedResultsWrittenToCohortTable(0),
      CohortItem(
        subscriptionName = expectedSubscriptionName,
        processingStage = NotificationSendProcessingOrError,
        whenNotificationSent = Some(expectedCurrentTime)
      )
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(1),
      CohortItem(
        subscriptionName = expectedSubscriptionName,
        processingStage = NotificationSendComplete,
        whenNotificationSent = Some(expectedCurrentTime)
      )
    )
  }

  test("NotificationHandler should fallback to using contact mailing address if no billing address") {
    val stubSalesforceClient =
      stubSFClient(List(salesforceSubscription), List(salesforceContact.copy(OtherAddress = None)))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val sentMessages = ArrayBuffer[EmailMessage]()
    val stubEmailSender = createStubEmailSender(sentMessages)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(brazeCampaignName)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_1, mailingAddressStreet)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_2, None)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_city, Some(mailingAddressCity))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_state, Some(mailingAddressState))
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_postal_code,
      mailingAddressPostalCode
    )
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_country, mailingAddressCountry)
  }

  test("NotificationHandler should send no message if no billing address or mailing address") {
    val stubSalesforceClient =
      stubSFClient(
        List(salesforceSubscription),
        List(salesforceContact.copy(OtherAddress = None, MailingAddress = None))
      )
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val sentMessages = ArrayBuffer[EmailMessage]()
    val stubEmailSender = createStubEmailSender(sentMessages)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(brazeCampaignName)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(sentMessages.size, 0)
    assertEquals(updatedResultsWrittenToCohortTable.size, 0)
  }

  test(
    "NotificationHandler should use salutation in place of first name and leave title field empty if contact has no first name"
  ) {
    val stubSalesforceClient =
      stubSFClient(List(salesforceSubscription), List(salesforceContact.copy(FirstName = None)))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val sentMessages = ArrayBuffer[EmailMessage]()
    val stubEmailSender = createStubEmailSender(sentMessages)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(brazeCampaignName)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.title, None)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.first_name, expectedSalutation)
  }

  test("NotificationHandler should leave CohortItem in processing state if email send fails") {
    val stubSalesforceClient = stubSFClient(List(salesforceSubscription), List(salesforceContact))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val failingStubEmailSender = createFailingStubEmailSender()

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(brazeCampaignName)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ failingStubEmailSender
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(updatedResultsWrittenToCohortTable.size, 1)
    assertEquals(
      updatedResultsWrittenToCohortTable(0),
      CohortItem(
        subscriptionName = expectedSubscriptionName,
        processingStage = NotificationSendProcessingOrError,
        whenNotificationSent = Some(expectedCurrentTime)
      )
    )
  }

  test("NotificationHandler should leave CohortItem in cancelled state if subscription is cancelled") {
    val cancelledSubscription = salesforceSubscription.copy(Status__c = "Cancelled")
    val stubSalesforceClient = stubSFClient(List(cancelledSubscription), List(salesforceContact))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val sentMessages = ArrayBuffer[EmailMessage]()
    val stubEmailSender = createStubEmailSender(sentMessages)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(brazeCampaignName)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(updatedResultsWrittenToCohortTable.size, 1)
    assertEquals(
      updatedResultsWrittenToCohortTable(0),
      CohortItem(
        subscriptionName = expectedSubscriptionName,
        processingStage = Cancelled
      )
    )
    assertEquals(sentMessages.size, 0)
  }
}
