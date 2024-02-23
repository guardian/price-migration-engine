package pricemigrationengine.handlers

import pricemigrationengine.handlers.NotificationHandler._
import pricemigrationengine.TestLogging
import pricemigrationengine.migrations.LegacyMigrations
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

import java.time.{Instant, LocalDate}
import scala.collection.mutable.ArrayBuffer

class NotificationHandlerTest extends munit.FunSuite {

  private val subscriptionName = "Sub-0001"
  private val startDate = LocalDate.of(2020, 1, 1)
  private val startDateUserFriendlyFormat = "1 January 2020"
  private val currency = "GBP"
  private val billingPeriod = "Month"
  private val billingPeriodInNotification = "Monthly"
  private val oldPrice = BigDecimal(10.00)

  // The estimated new price is the price without cap
  private val estimatedNewPrice = BigDecimal(15.00)
  test("For membership test, we need the estimatedNewPrice to be higher than the capped price") {
    assert(LegacyMigrations.priceCap(oldPrice, estimatedNewPrice) < estimatedNewPrice)
  }

  private val estimatedNewPriceWithCurrencySymbolPrefix = "£15.0"

  // Variation for Legacy migrations, where the price is capped to 10.00 * 1.2
  // What decides whether we are in a Legacy is the cohort specs name
  private val estimatedNewPriceWithCurrencySymbolPrefixLegacyVariation = "£12.00"

  private val sfSubscriptionId = "1234"
  private val buyerId = "buyer-1"
  private val identityId = "buyer1-identity-id"
  private val emailAddress = "buyer@email.address"
  private val firstName = "buyer1FirstName"
  private val lastName = "buyer1LastName"
  private val street = "buyer1Street"
  private val city = "buyer1City"
  private val state = "buyer1State"
  private val postalCode = "buyer1PostalCode"
  private val country = "buyer1Country"
  private val dataExtensionName = "SV_VO_Pricerise_Q22020"
  private val salutation = "Ms"
  private val sfStatus = "Active"
  private val productType = "Newspaper - Digital Voucher"
  private val currentTime = Instant.parse("2020-05-21T15:16:37Z")

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

  private def createStubZuora() = {
    ZLayer.succeed(
      new Zuora {

        override def fetchSubscription(subscriptionNumber: String): ZIO[Any, ZuoraFetchFailure, ZuoraSubscription] =
          ZIO.succeed(
            ZuoraSubscription(
              subscriptionNumber = "subscriptionNumber",
              version = 1,
              customerAcceptanceDate = LocalDate.of(2024, 2, 7),
              contractEffectiveDate = LocalDate.of(2024, 2, 7),
              ratePlans = List(),
              accountNumber = "accountNumber",
              accountId = "accountId",
              status = "Active",
              termStartDate = LocalDate.of(2024, 2, 7),
              termEndDate = LocalDate.of(2024, 2, 7)
            )
          )

        override def fetchAccount(
            accountNumber: String,
            subscriptionNumber: String
        ): ZIO[Any, ZuoraFetchFailure, ZuoraAccount] = ZIO.succeed(ZuoraAccount(SoldToContact("UK")))

        override def fetchInvoicePreview(
            accountId: String,
            targetDate: LocalDate
        ): ZIO[Any, ZuoraFetchFailure, ZuoraInvoiceList] = ZIO.succeed(ZuoraInvoiceList(List()))

        override val fetchProductCatalogue: ZIO[Any, ZuoraFetchFailure, ZuoraProductCatalogue] =
          ZIO.succeed(ZuoraProductCatalogue(Set(), None))

        override def updateSubscription(
            subscription: ZuoraSubscription,
            update: ZuoraSubscriptionUpdate
        ): ZIO[Any, ZuoraUpdateFailure, ZuoraSubscriptionId] = ZIO.succeed("ZuoraSubscriptionId")

        override def renewSubscription(subscriptionNumber: String): ZIO[Any, ZuoraRenewalFailure, Unit] =
          ZIO.succeed(())
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
      sfSubscriptionId,
      subscriptionName,
      buyerId,
      sfStatus,
      Some(productType)
    )

  private val salesforceContact: SalesforceContact =
    SalesforceContact(
      Id = buyerId,
      IdentityID__c = Some(identityId),
      Email = Some(emailAddress),
      Salutation = Some(salutation),
      FirstName = Some(firstName),
      LastName = Some(lastName),
      OtherAddress = Some(
        SalesforceAddress(
          street = Some(street),
          city = Some(city),
          state = Some(state),
          postalCode = Some(postalCode),
          country = Some(country)
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
      subscriptionName = subscriptionName,
      processingStage = AmendmentComplete,
      startDate = Some(startDate),
      currency = Some(currency),
      oldPrice = Some(oldPrice),
      estimatedNewPrice = Some(estimatedNewPrice),
      billingPeriod = Some(billingPeriod),
      whenEstimationDone = Some(Instant.parse("2023-02-07T15:38:26Z"))
    )

  test("guLettersNotificationLeadTime should be at least 49 days") {
    // "Should be at least 49 days", but for invariance we test against the
    // usual value of exactly 49
    val cohortSpec =
      CohortSpec("LegacyPrintProductName", brazeCampaignName, LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))
    assert(maxLeadTime(cohortSpec) == 49)
  }

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
          _ <- TestClock.setTime(currentTime)
          program <- NotificationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender ++ createStubZuora()
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(sentMessages.size, 1)
    assertEquals(sentMessages(0).DataExtensionName, dataExtensionName)
    assertEquals(sentMessages(0).SfContactId, buyerId)
    assertEquals(sentMessages(0).IdentityUserId, Some(identityId))
    assertEquals(sentMessages(0).To.Address, Some(emailAddress))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_1, street)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_2, None)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_city, Some(city))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_state, Some(state))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_postal_code, postalCode)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_country, country)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.title, Some(salutation))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.first_name, firstName)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.last_name, lastName)
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.payment_amount,
      estimatedNewPriceWithCurrencySymbolPrefixLegacyVariation
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.next_payment_date,
      startDateUserFriendlyFormat
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.payment_frequency,
      billingPeriodInNotification
    )
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.subscription_id, subscriptionName)

    assertEquals(updatedResultsWrittenToCohortTable.size, 2)
    assertEquals(
      updatedResultsWrittenToCohortTable(0),
      CohortItem(
        subscriptionName = subscriptionName,
        processingStage = NotificationSendProcessingOrError,
        whenNotificationSent = Some(currentTime)
      )
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(1),
      CohortItem(
        subscriptionName = subscriptionName,
        processingStage = NotificationSendComplete,
        whenNotificationSent = Some(currentTime)
      )
    )
  }

  test(
    "NotificationHandler should get records from cohort table and SF and send Email with the data (membership Batch1)"
  ) {

    // The membership variation here uses a similar structure as the legacy NotificationHandler test, but we need
    // to update:
    //     - the cohortItem, which has a specific startDate
    //     - the currentTime (which needs to have a particular value relatively to the start date, considering the
    //       shorter notification window)
    //     - the startDateUserFriendlyFormat

    val itemStartDate = LocalDate.of(2023, 5, 1)

    val cohortItem =
      CohortItem(
        subscriptionName = subscriptionName,
        processingStage = AmendmentComplete,
        startDate = Some(itemStartDate),
        currency = Some(currency),
        oldPrice = Some(oldPrice),
        estimatedNewPrice = Some(estimatedNewPrice),
        billingPeriod = Some(billingPeriod),
        whenEstimationDone = Some(Instant.parse("2023-02-07T15:38:26Z"))
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

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(dataCurrentTime)
          program <- NotificationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender ++ createStubZuora()
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(sentMessages.size, 1)
    assertEquals(sentMessages(0).DataExtensionName, dataExtensionName)
    assertEquals(sentMessages(0).SfContactId, buyerId)
    assertEquals(sentMessages(0).IdentityUserId, Some(identityId))
    assertEquals(sentMessages(0).To.Address, Some(emailAddress))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_1, street)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_2, None)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_city, Some(city))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_state, Some(state))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_postal_code, postalCode)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_country, country)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.title, Some(salutation))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.first_name, firstName)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.last_name, lastName)
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.payment_amount,
      estimatedNewPriceWithCurrencySymbolPrefix
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.next_payment_date,
      expectedStartDateUserFriendlyFormat
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.payment_frequency,
      billingPeriodInNotification
    )
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.subscription_id, subscriptionName)

    assertEquals(updatedResultsWrittenToCohortTable.size, 2)
    assertEquals(
      updatedResultsWrittenToCohortTable(0),
      CohortItem(
        subscriptionName = subscriptionName,
        processingStage = NotificationSendProcessingOrError,
        whenNotificationSent = Some(dataCurrentTime)
      )
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(1),
      CohortItem(
        subscriptionName = subscriptionName,
        processingStage = NotificationSendComplete,
        whenNotificationSent = Some(dataCurrentTime)
      )
    )
  }

  test(
    "NotificationHandler should get records from cohort table and SF and send Email with the data (membership Batch2)"
  ) {

    // This test is identical to the previous one, but specific to Batch2

    val itemStartDate = LocalDate.of(2023, 6, 1)

    val cohortItem =
      CohortItem(
        subscriptionName = subscriptionName,
        processingStage = AmendmentComplete,
        startDate = Some(itemStartDate),
        currency = Some(currency),
        oldPrice = Some(oldPrice),
        estimatedNewPrice = Some(estimatedNewPrice),
        billingPeriod = Some(billingPeriod),
        whenEstimationDone = Some(Instant.parse("2023-02-07T15:38:26Z"))
      )

    val dataCurrentTime = Instant.parse("2023-03-29T07:00:00Z")
    val expectedStartDateUserFriendlyFormat = "1 June 2023"

    val stubSalesforceClient = stubSFClient(List(salesforceSubscription), List(salesforceContact))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val sentMessages = ArrayBuffer[EmailMessage]()
    val stubEmailSender = createStubEmailSender(sentMessages)

    // Building the cohort spec with the correct campaign name (as during the previous test)
    // This time we also need to set the correct campaign name to trigger the membership price cap override
    val cohortSpec =
      CohortSpec("Membership2023_Batch2", brazeCampaignName, LocalDate.of(2000, 1, 1), LocalDate.of(2023, 6, 1))

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(dataCurrentTime)
          program <- NotificationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender ++ createStubZuora()
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(sentMessages.size, 1)
    assertEquals(sentMessages(0).DataExtensionName, dataExtensionName)
    assertEquals(sentMessages(0).SfContactId, buyerId)
    assertEquals(sentMessages(0).IdentityUserId, Some(identityId))
    assertEquals(sentMessages(0).To.Address, Some(emailAddress))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_1, street)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_address_2, None)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_city, Some(city))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_state, Some(state))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_postal_code, postalCode)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.billing_country, country)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.title, Some(salutation))
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.first_name, firstName)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.last_name, lastName)
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.payment_amount,
      estimatedNewPriceWithCurrencySymbolPrefix
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.next_payment_date,
      expectedStartDateUserFriendlyFormat
    )
    assertEquals(
      sentMessages(0).To.ContactAttributes.SubscriberAttributes.payment_frequency,
      billingPeriodInNotification
    )
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.subscription_id, subscriptionName)

    assertEquals(updatedResultsWrittenToCohortTable.size, 2)
    assertEquals(
      updatedResultsWrittenToCohortTable(0),
      CohortItem(
        subscriptionName = subscriptionName,
        processingStage = NotificationSendProcessingOrError,
        whenNotificationSent = Some(dataCurrentTime)
      )
    )
    assertEquals(
      updatedResultsWrittenToCohortTable(1),
      CohortItem(
        subscriptionName = subscriptionName,
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
          _ <- TestClock.setTime(currentTime)
          program <- NotificationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender ++ createStubZuora()
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
          _ <- TestClock.setTime(currentTime)
          program <- NotificationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender ++ createStubZuora()
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
          _ <- TestClock.setTime(currentTime)
          program <- NotificationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender ++ createStubZuora()
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.title, None)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.first_name, salutation)
  }

  test(
    "NotificationHandler, if membership price rise (Batch1), in case of missing FirstMame and missing Salutation, should still send an email"
  ) {
    val stubSalesforceClient =
      stubSFClient(List(salesforceSubscription), List(salesforceContact.copy(FirstName = None, Salutation = None)))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val sentMessages = ArrayBuffer[EmailMessage]()
    val stubEmailSender = createStubEmailSender(sentMessages)

    // Building the cohort spec with the correct campaign name
    val cohortSpec =
      CohortSpec("Membership2023_Batch1", brazeCampaignName, LocalDate.of(2000, 1, 1), LocalDate.of(2023, 5, 1))

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(currentTime)
          program <- NotificationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender ++ createStubZuora()
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(sentMessages.size, 1)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.title, None)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.first_name, "Member")
  }

  test(
    "NotificationHandler, if membership price rise (Batch2), in case of missing FirstMame and missing Salutation, should still send an email"
  ) {
    val stubSalesforceClient =
      stubSFClient(List(salesforceSubscription), List(salesforceContact.copy(FirstName = None, Salutation = None)))
    val updatedResultsWrittenToCohortTable = ArrayBuffer[CohortItem]()
    val stubCohortTable = createStubCohortTable(updatedResultsWrittenToCohortTable, cohortItem)
    val sentMessages = ArrayBuffer[EmailMessage]()
    val stubEmailSender = createStubEmailSender(sentMessages)

    // Building the cohort spec with the correct campaign name
    val cohortSpec =
      CohortSpec("Membership2023_Batch2", brazeCampaignName, LocalDate.of(2000, 1, 1), LocalDate.of(2023, 6, 1))

    assertEquals(
      unsafeRunSync(default)(
        (for {
          _ <- TestClock.setTime(currentTime)
          program <- NotificationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender ++ createStubZuora()
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(sentMessages.size, 1)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.title, None)
    assertEquals(sentMessages(0).To.ContactAttributes.SubscriberAttributes.first_name, "Member")
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
          _ <- TestClock.setTime(currentTime)
          program <- NotificationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ failingStubEmailSender ++ createStubZuora()
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(updatedResultsWrittenToCohortTable.size, 1)
    assertEquals(
      updatedResultsWrittenToCohortTable(0),
      CohortItem(
        subscriptionName = subscriptionName,
        processingStage = NotificationSendProcessingOrError,
        whenNotificationSent = Some(currentTime)
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
          _ <- TestClock.setTime(currentTime)
          program <- NotificationHandler.main(cohortSpec)
        } yield program).provideLayer(
          testEnvironment ++ TestLogging.logging ++ stubCohortTable ++ stubSalesforceClient ++ stubEmailSender ++ createStubZuora()
        )
      ),
      Success(HandlerOutput(isComplete = true))
    )

    assertEquals(updatedResultsWrittenToCohortTable.size, 1)
    assertEquals(
      updatedResultsWrittenToCohortTable(0),
      CohortItem(
        subscriptionName = subscriptionName,
        processingStage = Cancelled
      )
    )
    assertEquals(sentMessages.size, 0)
  }

  test("thereIsEnoughNotificationLeadTime behaves correctly (legacy case)") {
    // The item startDate will be set for May 4th
    // In the legacy case, of 35 days min lead time:
    //     - May 1st should be enough lead time (although not yet in the notification window)
    //     - April 1st should be not be enough (there only is 33 days from April 1st to May 4th)

    // (We are going to use the same values for the membership migration, where we will observing that May
    // 1st will be enough, but May 5th won't)

    val itemStartDate = LocalDate.of(2023, 5, 4)

    val cohortSpec = CohortSpec("CohortName", "BrazeCampaignName", LocalDate.of(2000, 1, 1), itemStartDate)
    val cohortItem = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate))

    // The two following dates are chosen to be after 1st Dec 2022, to hit the non trivial case of the check
    val date2 = LocalDate.of(2023, 3, 1)
    val date3 = LocalDate.of(2023, 4, 1)
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date2, cohortItem), true)
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date3, cohortItem), false)
  }

  test("thereIsEnoughNotificationLeadTime behaves correctly (membership case, Batch1)") {
    // We are using the same dates as for the previous test (legacy case)

    // Here let's observe the slight shift in notification period due to membership variation, by which
    // 33 days wasn't enough in the legacy case, but will be in the membership case. We also test with 30 days
    // to observe that it won't be enough.

    // Note that in the case of membership the notification period is -33 (included) to -31 (excluded) days
    // For more details about why -33 is included but -31 is excluded, see explanation at the top of the Notification
    // handler.

    val itemStartDate = LocalDate.of(2023, 5, 4)

    val cohortSpec = CohortSpec("Membership2023_Batch1", "BrazeCampaignName", LocalDate.of(2000, 1, 1), itemStartDate)
    val cohortItem = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate))

    val date2 = LocalDate.of(2023, 3, 1) // true
    val date3 = LocalDate.of(2023, 4, 1) // 33 days to target, should true
    val date4 = LocalDate.of(2023, 4, 4) // 30 days to target, should false
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date2, cohortItem), true)
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date3, cohortItem), true)
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date4, cohortItem), false)
  }

  test("thereIsEnoughNotificationLeadTime behaves correctly (membership case, Batch2)") {
    // Similar as Batch1, but shifted by a month

    // Note that in the case of membership the notification period is -33 (included) to -31 (excluded) days
    // For more details about why -33 is included but -31 is excluded, see explanation at the top of the Notification
    // handler.

    val itemStartDate = LocalDate.of(2023, 6, 4)

    val cohortSpec = CohortSpec("Membership2023_Batch2", "BrazeCampaignName", LocalDate.of(2000, 1, 1), itemStartDate)
    val cohortItem = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate))

    val date2 = LocalDate.of(2023, 4, 1) // true
    val date3 = LocalDate.of(2023, 5, 1) // 34 days to target, should true
    val date4 = LocalDate.of(2023, 5, 2) // 33 days to target, should true
    val date5 = LocalDate.of(2023, 5, 3) // 32 days to target, should true
    val date6 = LocalDate.of(2023, 5, 4) // 31 days to target, should false
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date2, cohortItem), true)
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date3, cohortItem), true)
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date4, cohortItem), true)
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date5, cohortItem), true)
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, date6, cohortItem), false)

  }

  test("thereIsEnoughNotificationLeadTime behaves correctly (membership case, Batch3)") {
    // Here we are testing and calibrating the timing required for a start of emailing on 5 July 2023
    // Sending a couple on the 5th that will need to be time cleared (at -33 days)

    val today = LocalDate.of(2023, 7, 5)

    val itemStartDate1 = LocalDate.of(2023, 8, 4) // +30 days
    val itemStartDate2 = LocalDate.of(2023, 8, 5) // +31 days
    val itemStartDate3 = LocalDate.of(2023, 8, 6) // +32 days
    val itemStartDate4 = LocalDate.of(2023, 8, 7) // +33 days
    val itemStartDate5 = LocalDate.of(2023, 8, 8) // +34 days

    val cohortItem1 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate1))
    val cohortItem2 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate2))
    val cohortItem3 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate3))
    val cohortItem4 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate4))
    val cohortItem5 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate5))

    val cohortSpec =
      CohortSpec("Membership2023_Batch3", "BrazeCampaignName", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 1, 1))

    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem1), false) // +30 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem2), false) // +31 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem3), true) // +32 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem4), true) // +33 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem5), true) // +34 days
  }

  test("thereIsEnoughNotificationLeadTime behaves correctly (supporter plus 2023)") {
    // Here we are testing and calibrating the timing required for a start of emailing on 22 August 2023
    // Process starting 20 July 2023

    val today = LocalDate.of(2023, 7, 20)

    val itemStartDate1 = LocalDate.of(2023, 8, 19) // +30 days
    val itemStartDate2 = LocalDate.of(2023, 8, 20) // +31 days
    val itemStartDate3 = LocalDate.of(2023, 8, 21) // +32 days
    val itemStartDate4 = LocalDate.of(2023, 8, 22) // +33 days
    val itemStartDate5 = LocalDate.of(2023, 8, 23) // +34 days

    val cohortItem1 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate1))
    val cohortItem2 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate2))
    val cohortItem3 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate3))
    val cohortItem4 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate4))
    val cohortItem5 = CohortItem("subscriptionNumber", SalesforcePriceRiceCreationComplete, Some(itemStartDate5))

    val cohortSpec =
      CohortSpec("Membership2023_Batch3", "BrazeCampaignName", LocalDate.of(2000, 1, 1), LocalDate.of(2023, 8, 22))

    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem1), false) // +30 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem2), false) // +31 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem3), true) // +32 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem4), true) // +33 days
    assertEquals(thereIsEnoughNotificationLeadTime(cohortSpec, today, cohortItem5), true) // +34 days
  }
}
