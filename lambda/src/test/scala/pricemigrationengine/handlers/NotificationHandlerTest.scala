package pricemigrationengine.handlers

import pricemigrationengine.handlers.NotificationHandler.thereIsEnoughNotificationLeadTime
import pricemigrationengine.{TestLogging}
import pricemigrationengine.model.CohortTableFilter._
import pricemigrationengine.model._
import pricemigrationengine.model.membershipworkflow.EmailMessage
import pricemigrationengine.services._
import pricemigrationengine.util.Runner.unsafeRunSync
import zio.Exit.Success
import zio.Runtime.default
import zio.stream.ZStream
import zio.test.{TestClock, testEnvironment}
import zio.{IO, ZIO, ZLayer}

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneOffset}
import scala.collection.mutable.ArrayBuffer

class NotificationHandlerTest extends munit.FunSuite {

  private val expectedSubscriptionName = "Sub-0001"
  private val expectedStartDate = LocalDate.of(2020, 1, 1)
  private val expectedStartDateUserFriendlyFormat = "1 January 2020"
  private val expectedCurrency = "GBP"
  private val expectedBillingPeriod = "Month"
  private val expectedBillingPeriodInNotification = "Monthly"
  private val expectedOldPrice = BigDecimal(10.00)

  // The estimated new price is the price without cap
  private val expectedEstimatedNewPrice = BigDecimal(15.00)
  test("For membership test, we need the expectedEstimatedNewPrice to be higher than the capped price") {
    assert(PriceCap.cappedPrice(expectedOldPrice, expectedEstimatedNewPrice) < expectedEstimatedNewPrice)
  }

  // The price that is displayed to the customer is capped using the old price as base
  private val expectedCappedEstimatedNewPriceWithCurrencySymbolPrefix = "£12.00"

  // Membership variation
  // Also, for some reasons we only have one "0" here
  private val expectedUnCappedEstimatedNewPriceWithCurrencySymbolPrefix = "£15.0"

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

    // Building the cohort spec with the correct campaign name
    val cohortSpec = CohortSpec("Name", brazeCampaignName, LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(cohortSpec)
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
      expectedCappedEstimatedNewPriceWithCurrencySymbolPrefix
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.next_payment_date,
      expectedStartDateUserFriendlyFormat
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

  test(
    "NotificationHandler should get records from cohort table and SF and send Email with the data (membership variation)"
  ) {

    // The membership variation here uses a similar structure as the legacy NotificationHandler test, but we need
    // to update:
    //     - the cohortItem, which has a specific startDate
    //     - the currentTime (which needs to have a particular value relatively to the start date, considering the
    //       shorter notification window)
    //     - the expectedStartDateUserFriendlyFormat

    val itemStartDate = LocalDate.of(2023, 5, 1)

    val cohortItem =
      CohortItem(
        subscriptionName = expectedSubscriptionName,
        processingStage = AmendmentComplete,
        startDate = Some(itemStartDate),
        currency = Some(expectedCurrency),
        oldPrice = Some(expectedOldPrice),
        estimatedNewPrice = Some(expectedEstimatedNewPrice),
        billingPeriod = Some(expectedBillingPeriod)
      )

    val dataCurrentTime = Instant.parse("2023-03-29T07:00:00Z")
    val expectedStartDateUserFriendlyFormat = "1 May 2023"

    val stubSalesforceClient = stubSFClient(List(salesforceSubscription), List(salesforceContact))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val sentMessages = ArrayBuffer[EmailMessage]()
    val stubEmailSender = createStubEmailSender(sentMessages)

    // Building the cohort spec with the correct campaign name (as during the previous test)
    // This time we also need to set the correct campaign name to trigger the membership price cap override
    val cohortSpec =
      CohortSpec("Membership2023_Batch1", brazeCampaignName, LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))

    assertEquals(1, 1)

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(dataCurrentTime)
          program <- NotificationHandler.main(cohortSpec)
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
      expectedUnCappedEstimatedNewPriceWithCurrencySymbolPrefix
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.next_payment_date,
      expectedStartDateUserFriendlyFormat
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
        whenNotificationSent = Some(dataCurrentTime)
      )
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(1),
      CohortItem(
        subscriptionName = expectedSubscriptionName,
        processingStage = NotificationSendComplete,
        whenNotificationSent = Some(dataCurrentTime)
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

    // Building the cohort spec with the correct campaign name
    val cohortSpec = CohortSpec("Name", brazeCampaignName, LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(cohortSpec)
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

    // Building the cohort spec with the correct campaign name
    val cohortSpec = CohortSpec("Name", brazeCampaignName, LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(cohortSpec)
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

    // Building the cohort spec with the correct campaign name
    val cohortSpec = CohortSpec("Name", brazeCampaignName, LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(cohortSpec)
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

    // Building the cohort spec with the correct campaign name
    val cohortSpec = CohortSpec("Name", brazeCampaignName, LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(cohortSpec)
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

    // Building the cohort spec with the correct campaign name
    val cohortSpec = CohortSpec("Name", brazeCampaignName, LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(expectedCurrentTime)
          program <- NotificationHandler.main(cohortSpec)
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

  test("thereIsEnoughNotificationLeadTime behaves correctly (legacy case)") {
    // The item startDate will be set for April 4th
    // In the legacy case, of 35 days min lead time:
    //     - Feb 1st should be enough lead time (although not yet in the notification window)
    //     - May 1st should be not be enough (there only is 34 days from May 1st to April 4th)

    // (We are going to use the same values for the membership migration, where we will observing that May
    // 1st will be enough, but May 5th won't)

    val itemStartDate = LocalDate.of(2023, 4, 4)

    val cohortSpec = CohortSpec("CohortName", "BrazeCampaignName", LocalDate.of(2000, 1, 1), itemStartDate)
    val cohortItem = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate))

    // The two following dates are chosen to be after 1st Dec 2022, to hit the non trivial case of the check
    val date2 = LocalDate.of(2023, 2, 1)
    val date3 = LocalDate.of(2023, 3, 1)
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date2, cohortItem), true)
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date3, cohortItem), false)
  }

}
