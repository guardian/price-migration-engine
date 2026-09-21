package pricemigrationengine.model

import pricemigrationengine.migrations.{
  DigiSubs2025Migration,
  GuardianWeekly2025Migration,
  Membership2025Migration,
  Newspaper2025P1Migration,
  Newspaper2025P1NotificationData,
  Newspaper2025P3Migration,
  Newspaper2025P3NotificationData,
  SP2026EmailExtraAttributes,
  SupporterPlus2026Migration
}

import java.time.LocalDate
import pricemigrationengine.model.membershipworkflow.{
  BrazeMessage,
  BrazePayload,
  BrazePayloadContactAttributes,
  BrazePayloadSubscriberAttributes
}

import java.time.format.DateTimeFormatter

object NotificationHandlerHelper {

  // We end the notification window 30 days before the amendment date
  // This is a legal requirement
  val endOfNotificationWindow = 30

  def notificationLeadTime(cohortSpec: CohortSpec): Int = {
    MigrationType(cohortSpec) match {
      case Test1                         => 35
      case GuardianWeekly2025            => GuardianWeekly2025Migration.notificationLeadTime
      case Newspaper2025P1               => Newspaper2025P1Migration.notificationLeadTime
      case Newspaper2025P3               => Newspaper2025P3Migration.notificationLeadTime
      case Membership2025                => Membership2025Migration.notificationLeadTime
      case DigiSubs2025                  => DigiSubs2025Migration.notificationLeadTime
      case SupporterPlus2026             => SupporterPlus2026Migration.notificationLeadTime
      case Print2026C1GWAnnualsUK        => 35
      case Print2026C1GWQuarterliesUK    => 35
      case Print2026C1NPAnnualsUK        => 35
      case Print2026C1NPQuarterliesUK    => 35
      case Print2026C1NPSemiannualsUK    => 35
      case Print2026C2NPMonthliesUK      => 35
      case Print2026C3GWMonthliesUK      => 35
      case Print2026C3NPMonthliesUK      => 35
      case Print2026C4NPMonthliesUK      => 35
      case Print2026C5GW                 => 39
      case Print2026C5NP                 => 39
      case Print2026C6GWQuarterliesNonUK => 35
    }
  }

  def isNonTrivialValue(value: Option[String]): Boolean = {
    value.isDefined && value.get.nonEmpty
  }

  def messageIsWellFormed(cohortSpec: CohortSpec, message: BrazeMessage): Boolean = {
    // This function return whether or not an BrazeMessage is "well formed". And for the moment
    // this is limited to checking that the special circumstances extra attributes (which were
    // originally introduced for the Summer 2025 print migrations) are not empty.

    MigrationType(cohortSpec) match {
      case Test1              => true
      case GuardianWeekly2025 => true
      case Newspaper2025P1    => {
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2025_brand_title)
        ).forall(identity)
      }
      case Newspaper2025P3 => {
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2025_phase3_brand_title)
        ).forall(identity)
      }
      case Membership2025             => true
      case DigiSubs2025               => true
      case SupporterPlus2026          => true
      case Print2026C1GWAnnualsUK     => true
      case Print2026C1GWQuarterliesUK => true
      case Print2026C1NPAnnualsUK     =>
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2026_brand_title)
        ).forall(identity)
      case Print2026C1NPQuarterliesUK =>
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2026_brand_title)
        ).forall(identity)
      case Print2026C1NPSemiannualsUK =>
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2026_brand_title)
        ).forall(identity)
      case Print2026C2NPMonthliesUK =>
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2026_brand_title)
        ).forall(identity)
      case Print2026C3GWMonthliesUK => true
      case Print2026C3NPMonthliesUK =>
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2026_brand_title)
        ).forall(identity)
      case Print2026C4NPMonthliesUK =>
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2026_brand_title)
        ).forall(identity)
      case Print2026C5GW => true
      case Print2026C5NP =>
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2026_brand_title)
        ).forall(identity)
      case Print2026C6GWQuarterliesNonUK => true
    }
  }

  def thereIsEnoughNotificationLeadTime(cohortSpec: CohortSpec, today: LocalDate, cohortItem: CohortItem): Boolean = {
    // To help with backward compatibility with existing tests, we apply this condition from 1st Dec 2020.
    if (today.isBefore(LocalDate.of(2020, 12, 1))) {
      true
    } else {
      cohortItem.amendmentEffectiveDate match {
        case Some(sd) => today.plusDays(endOfNotificationWindow).isBefore(sd)
        case _        => false
      }
    }
  }

  private def requiredData[A](field: Option[A], fieldName: String): Either[NotificationHandlerFailure, A] = {
    field match {
      case Some(value) => Right(value)
      case None        => Left(NotificationHandlerFailure(s"$fieldName is a required field"))
    }
  }

  private def nonRequiredData[A](field: Option[A], defaultValue: A): Either[NotificationHandlerFailure, A] = {
    field match {
      case Some(value) => Right(value)
      case None        => Right(defaultValue)
    }
  }

  def evaluateStreet(
      cohortSpec: CohortSpec,
      street: Option[String]
  ): Either[NotificationHandlerFailure, String] = {
    MigrationType(cohortSpec) match {
      case Test1                         => requiredData(street, "Contact.OtherAddress.street")
      case GuardianWeekly2025            => requiredData(street, "Contact.OtherAddress.street")
      case Newspaper2025P1               => requiredData(street, "Contact.OtherAddress.street")
      case Newspaper2025P3               => requiredData(street, "Contact.OtherAddress.street")
      case Membership2025                => nonRequiredData(street, "")
      case DigiSubs2025                  => nonRequiredData(street, "")
      case SupporterPlus2026             => nonRequiredData(street, "")
      case Print2026C1GWAnnualsUK        => nonRequiredData(street, "")
      case Print2026C1GWQuarterliesUK    => nonRequiredData(street, "")
      case Print2026C1NPAnnualsUK        => nonRequiredData(street, "")
      case Print2026C1NPQuarterliesUK    => nonRequiredData(street, "")
      case Print2026C1NPSemiannualsUK    => nonRequiredData(street, "")
      case Print2026C2NPMonthliesUK      => nonRequiredData(street, "")
      case Print2026C3GWMonthliesUK      => nonRequiredData(street, "")
      case Print2026C3NPMonthliesUK      => nonRequiredData(street, "")
      case Print2026C4NPMonthliesUK      => nonRequiredData(street, "")
      case Print2026C5GW                 => requiredData(street, "Contact.OtherAddress.street") // [1]
      case Print2026C5NP                 => requiredData(street, "Contact.OtherAddress.street") // [1]
      case Print2026C6GWQuarterliesNonUK => nonRequiredData(street, "")
      // Cohort 5, is DM (letters)
    }
  }

  def decideCountry(
      cohortSpec: CohortSpec,
      notificationAddress: NotificationAddress
  ): Either[NotificationHandlerFailure, String] = {
    MigrationType(cohortSpec) match {
      case Test1                  => requiredData(notificationAddress.country, "Contact.OtherAddress.country")
      case GuardianWeekly2025     => requiredData(notificationAddress.country, "Contact.OtherAddress.country")
      case Newspaper2025P1        => nonRequiredData(notificationAddress.country, "United Kingdom")
      case Newspaper2025P3        => nonRequiredData(notificationAddress.country, "United Kingdom")
      case Membership2025         => nonRequiredData(notificationAddress.country, "")
      case DigiSubs2025           => nonRequiredData(notificationAddress.country, "")
      case SupporterPlus2026      => nonRequiredData(notificationAddress.country, "")
      case Print2026C1GWAnnualsUK => requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [1]
      case Print2026C1GWQuarterliesUK =>
        requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [1]
      case Print2026C1NPAnnualsUK => requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [1]
      case Print2026C1NPQuarterliesUK =>
        requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [1]
      case Print2026C1NPSemiannualsUK =>
        requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [1]
      case Print2026C2NPMonthliesUK => requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [1]
      case Print2026C3GWMonthliesUK => requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [1]
      case Print2026C3NPMonthliesUK => requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [1]
      case Print2026C4NPMonthliesUK => requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [1]
      case Print2026C5GW            => requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [2]
      case Print2026C5NP            => requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [2]
      case Print2026C6GWQuarterliesNonUK =>
        requiredData(notificationAddress.country, "Contact.OtherAddress.country") // [1]
    }

    // [1] not used in the template, but used in the canvas logic
    // [2] used for the post address
  }

  def decideFirstName(contact: SalesforceContact): Either[NotificationHandlerFailure, String] = {
    requiredData(contact.FirstName, "Contact.FirstName").left
      .flatMap(_ => requiredData(contact.Salutation.fold(Some("Member"))(Some(_)), "Contact.Salutation"))
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

  def decideBrazeName(
      cohortSpec: CohortSpec,
      item: CohortItem,
      zuoraSubscription: ZuoraSubscription
  ): Option[String] = {
    MigrationType(cohortSpec) match {
      case Test1                         => Some("unspecified")
      case GuardianWeekly2025            => Some("SV_GW_PriceRise2025")
      case Newspaper2025P1               => Some("SV_NP_PriceRise_2025")
      case Newspaper2025P3               => Some("SV_NP_PriceRise_VoucherSubCard2025")
      case Membership2025                => Membership2025Migration.brazeName(item)
      case DigiSubs2025                  => DigiSubs2025Migration.brazeName(item)
      case SupporterPlus2026             => SupporterPlus2026Migration.brazeName(item, zuoraSubscription)
      case Print2026C1GWAnnualsUK        => Some("SV_GW_PriceRise2026")
      case Print2026C1GWQuarterliesUK    => Some("SV_GW_PriceRise2026")
      case Print2026C1NPAnnualsUK        => Some("SV_NP_PriceRise_2026")
      case Print2026C1NPQuarterliesUK    => Some("SV_NP_PriceRise_2026")
      case Print2026C1NPSemiannualsUK    => Some("SV_NP_PriceRise_2026")
      case Print2026C2NPMonthliesUK      => Some("SV_NP_PriceRise_2026")
      case Print2026C3GWMonthliesUK      => Some("SV_GW_PriceRise2026")
      case Print2026C3NPMonthliesUK      => Some("SV_NP_PriceRise_2026")
      case Print2026C4NPMonthliesUK      => Some("SV_NP_PriceRise_2026")
      case Print2026C5GW                 => Some("SV_GW_PriceRiseDM_2026")
      case Print2026C5NP                 => Some("SV_NP_PriceRiseDM_2026")
      case Print2026C6GWQuarterliesNonUK => Some("SV_GW_PriceRise2026")
    }
  }

  def buildBrazeMessage(
      contact: SalesforceContact,
      firstName: String,
      lastName: String,
      street: String,
      notificationAddress: NotificationAddress,
      postalCode: String,
      country: String,
      commsPriceWithCurrencySymbol: String,
      amendmentEffectiveDate: String,
      paymentFrequency: String,
      cohortItem: CohortItem,
      sfSubscription: SalesforceSubscription,
      newspaper2025P1NotificationData: Newspaper2025P1NotificationData,
      newspaper2025P3NotificationData: Newspaper2025P3NotificationData,
      currencySymbol: String,
      supporterPlus2026ExtraData: SP2026EmailExtraAttributes,
      newspaper2026_brand_title: String,
      brazeName: String
  ): BrazeMessage = {
    BrazeMessage(
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
            billing_city = notificationAddress.city,
            billing_postal_code = postalCode,
            billing_state = notificationAddress.state,
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
  }

  def zuoraAccountSoldToContactToStreetInformation(
      zuoraAccountSoldToContact: ZuoraAccountSoldToContact
  ): Option[String] = {
    // Date: September 2026
    // Author: Pascal
    // The ZuoraAccountSoldToContact comes with address1 and address2 as
    // defined in the Zuora schema but the newly introduced NotificationAddress
    // is defined with a single optional streetInformation mostly due to the fact that
    // the BrazePayloadSubscriberAttributes billing_address_2 is set to None,
    // but this is something I should investigate and challenge one day
    (zuoraAccountSoldToContact.address1, zuoraAccountSoldToContact.address2) match {
      case (Some(a), Some(b)) => Some(s"$a / $b")
      case (Some(a), None)    => Some(a)
      case (None, Some(b))    => Some(b)
      case (None, None)       => None
    }
  }

  def firstDefined[A](options: Option[A]*): Option[A] =
    options.foldLeft(Option.empty[A])(_.orElse(_))

  def buildNotificationAddress(
      zuoraAccountSoldToContact: ZuoraAccountSoldToContact,
      salesforceContact: SalesforceContact
  ): NotificationAddress = {
    // For the selection sequence rationale see docs/postal-addresses.md

    val solution1 =
      for {
        streetInformation <- zuoraAccountSoldToContactToStreetInformation(zuoraAccountSoldToContact)
        _ <- zuoraAccountSoldToContact.city
      } yield NotificationAddress(
        streetInformation = Some(streetInformation),
        city = zuoraAccountSoldToContact.city,
        state = zuoraAccountSoldToContact.state,
        postalCode = zuoraAccountSoldToContact.zipCode,
        country = Some(zuoraAccountSoldToContact.country)
      )

    val solution2 = for {
      data <- salesforceContact.MailingAddress
      streetInformation <- data.street
      _ <- data.city
    } yield NotificationAddress(
      streetInformation = Some(streetInformation),
      city = data.city,
      state = data.state,
      postalCode = data.postalCode,
      country = data.country
    )

    val solution3 =
      for {
        data <- salesforceContact.OtherAddress
        streetInformation <- data.street
      } yield NotificationAddress(
        streetInformation = Some(streetInformation),
        city = data.city,
        state = data.state,
        postalCode = data.postalCode,
        country = data.country
      )

    firstDefined(solution1, solution2, solution3).getOrElse(NotificationAddress(None, None, None, None, None))

  }
}
