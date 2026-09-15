package pricemigrationengine.model

import pricemigrationengine.migrations.{
  DigiSubs2025Migration,
  GuardianWeekly2025Migration,
  Membership2025Migration,
  Newspaper2025P1Migration,
  Newspaper2025P3Migration,
  ProductMigration2025N4Migration,
  SupporterPlus2026Migration
}

import java.time.LocalDate
import pricemigrationengine.model.membershipworkflow.BrazeMessage

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
      case ProductMigration2025N4        => ProductMigration2025N4Migration.notificationLeadTime
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
      case ProductMigration2025N4 => {
        List(
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2025_phase4_brand_title),
          isNonTrivialValue(message.To.ContactAttributes.SubscriberAttributes.newspaper2025_phase4_formstack_url),
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

  private def requiredField[A](field: Option[A], fieldName: String): Either[NotificationHandlerFailure, A] = {
    field match {
      case Some(value) => Right(value)
      case None        => Left(NotificationHandlerFailure(s"$fieldName is a required field"))
    }
  }

  private def nonRequiredField[A](field: Option[A], defaultValue: A): Either[NotificationHandlerFailure, A] = {
    field match {
      case Some(value) => Right(value)
      case None        => Right(defaultValue)
    }
  }

  def targetStreet(cohortSpec: CohortSpec, street: Option[String]): Either[NotificationHandlerFailure, String] = {
    MigrationType(cohortSpec) match {
      case Test1                         => requiredField(street, "Contact.OtherAddress.street")
      case GuardianWeekly2025            => requiredField(street, "Contact.OtherAddress.street")
      case Newspaper2025P1               => requiredField(street, "Contact.OtherAddress.street")
      case Newspaper2025P3               => requiredField(street, "Contact.OtherAddress.street")
      case ProductMigration2025N4        => requiredField(street, "Contact.OtherAddress.street")
      case Membership2025                => nonRequiredField(street, "")
      case DigiSubs2025                  => nonRequiredField(street, "")
      case SupporterPlus2026             => nonRequiredField(street, "")
      case Print2026C1GWAnnualsUK        => nonRequiredField(street, "")
      case Print2026C1GWQuarterliesUK    => nonRequiredField(street, "")
      case Print2026C1NPAnnualsUK        => nonRequiredField(street, "")
      case Print2026C1NPQuarterliesUK    => nonRequiredField(street, "")
      case Print2026C1NPSemiannualsUK    => nonRequiredField(street, "")
      case Print2026C2NPMonthliesUK      => nonRequiredField(street, "")
      case Print2026C3GWMonthliesUK      => nonRequiredField(street, "")
      case Print2026C3NPMonthliesUK      => nonRequiredField(street, "")
      case Print2026C4NPMonthliesUK      => nonRequiredField(street, "")
      case Print2026C5GW                 => requiredField(street, "Contact.OtherAddress.street") // [1]
      case Print2026C5NP                 => requiredField(street, "Contact.OtherAddress.street") // [1]
      case Print2026C6GWQuarterliesNonUK => nonRequiredField(street, "")
      // Cohort 5, is DM (letters)
    }
  }

  def country(
      cohortSpec: CohortSpec,
      address: SalesforceAddress
  ): Either[NotificationHandlerFailure, String] = {
    MigrationType(cohortSpec) match {
      case Test1                         => requiredField(address.country, "Contact.OtherAddress.country")
      case GuardianWeekly2025            => requiredField(address.country, "Contact.OtherAddress.country")
      case Newspaper2025P1               => nonRequiredField(address.country, "United Kingdom")
      case Newspaper2025P3               => nonRequiredField(address.country, "United Kingdom")
      case ProductMigration2025N4        => nonRequiredField(address.country, "")
      case Membership2025                => nonRequiredField(address.country, "")
      case DigiSubs2025                  => nonRequiredField(address.country, "")
      case SupporterPlus2026             => nonRequiredField(address.country, "")
      case Print2026C1GWAnnualsUK        => requiredField(address.country, "Contact.OtherAddress.country") // [1]
      case Print2026C1GWQuarterliesUK    => requiredField(address.country, "Contact.OtherAddress.country") // [1]
      case Print2026C1NPAnnualsUK        => requiredField(address.country, "Contact.OtherAddress.country") // [1]
      case Print2026C1NPQuarterliesUK    => requiredField(address.country, "Contact.OtherAddress.country") // [1]
      case Print2026C1NPSemiannualsUK    => requiredField(address.country, "Contact.OtherAddress.country") // [1]
      case Print2026C2NPMonthliesUK      => requiredField(address.country, "Contact.OtherAddress.country") // [1]
      case Print2026C3GWMonthliesUK      => requiredField(address.country, "Contact.OtherAddress.country") // [1]
      case Print2026C3NPMonthliesUK      => requiredField(address.country, "Contact.OtherAddress.country") // [1]
      case Print2026C4NPMonthliesUK      => requiredField(address.country, "Contact.OtherAddress.country") // [1]
      case Print2026C5GW                 => requiredField(address.country, "Contact.OtherAddress.country") // [2]
      case Print2026C5NP                 => requiredField(address.country, "Contact.OtherAddress.country") // [2]
      case Print2026C6GWQuarterliesNonUK => requiredField(address.country, "Contact.OtherAddress.country") // [1]
    }

    // [1] not used in the template, but used in the canvas logic
    // [2] used for the post address
  }

  def firstName(contact: SalesforceContact): Either[NotificationHandlerFailure, String] = {
    requiredField(contact.FirstName, "Contact.FirstName").left
      .flatMap(_ => requiredField(contact.Salutation.fold(Some("Member"))(Some(_)), "Contact.Salutation"))
  }

  private def targetAddressNotRequired(
      contact: SalesforceContact
  ): Either[NotificationHandlerFailure, SalesforceAddress] = {
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

  private def targetAddressRequired(
      contact: SalesforceContact
  ): Either[NotificationHandlerFailure, SalesforceAddress] = {
    (for {
      billingAddress <- requiredField(contact.OtherAddress, "Contact.OtherAddress")
      _ <- requiredField(billingAddress.street, "Contact.OtherAddress.street")
      _ <- requiredField(billingAddress.city, "Contact.OtherAddress.city")
    } yield billingAddress).left.flatMap(_ => requiredField(contact.MailingAddress, "Contact.MailingAddress"))
  }

  def targetAddress(
      cohortSpec: CohortSpec,
      contact: SalesforceContact
  ): Either[NotificationHandlerFailure, SalesforceAddress] = {
    MigrationType(cohortSpec) match {
      case Test1                         => targetAddressRequired(contact)
      case GuardianWeekly2025            => targetAddressRequired(contact)
      case Newspaper2025P1               => targetAddressRequired(contact)
      case Newspaper2025P3               => targetAddressNotRequired(contact)
      case ProductMigration2025N4        => targetAddressNotRequired(contact)
      case Membership2025                => targetAddressNotRequired(contact)
      case DigiSubs2025                  => targetAddressNotRequired(contact)
      case SupporterPlus2026             => targetAddressNotRequired(contact)
      case Print2026C1GWAnnualsUK        => targetAddressNotRequired(contact)
      case Print2026C1GWQuarterliesUK    => targetAddressNotRequired(contact)
      case Print2026C1NPAnnualsUK        => targetAddressNotRequired(contact)
      case Print2026C1NPQuarterliesUK    => targetAddressNotRequired(contact)
      case Print2026C1NPSemiannualsUK    => targetAddressNotRequired(contact)
      case Print2026C2NPMonthliesUK      => targetAddressNotRequired(contact)
      case Print2026C3GWMonthliesUK      => targetAddressNotRequired(contact)
      case Print2026C3NPMonthliesUK      => targetAddressNotRequired(contact)
      case Print2026C4NPMonthliesUK      => targetAddressNotRequired(contact)
      case Print2026C5GW                 => targetAddressRequired(contact)
      case Print2026C5NP                 => targetAddressRequired(contact)
      case Print2026C6GWQuarterliesNonUK => targetAddressNotRequired(contact)
    }
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
      case ProductMigration2025N4        => ProductMigration2025N4Migration.brazeName(item)
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
}

sealed trait SubscriptionNotificationAnalyseResult

// "SNAR" means "Subscription Notification Analyse Result"

object SNARReadyToNotify extends SubscriptionNotificationAnalyseResult
object SNARCancelledInZuora extends SubscriptionNotificationAnalyseResult
object SNARExcludeFromMigration extends SubscriptionNotificationAnalyseResult
object SNARMissingNotificationWindow extends SubscriptionNotificationAnalyseResult

object SubscriptionNotificationAnalyseResult {

  def toString(result: SubscriptionNotificationAnalyseResult): String = {
    result match {
      case SNARReadyToNotify             => "SNARReadyToNotify"
      case SNARCancelledInZuora          => "SNARCancelledInZuora"
      case SNARExcludeFromMigration      => "SNARExcludeFromMigration"
      case SNARMissingNotificationWindow => "SNARMissingNotificationWindow"
    }
  }

  def analyseSubscriptionForNotification_Legacy(
      ratePlanProbeResult: RatePlanProbeResult
  ): Option[SubscriptionNotificationAnalyseResult] = {
    ratePlanProbeResult match {
      case RPPShouldProceed        => Some(SNARReadyToNotify)
      case RPPCancelledInZuora     => Some(SNARCancelledInZuora)
      case IndeterminateConclusion => None
    }
  }

  def analyseSubscriptionForNotification_SupporterPlus2026(
      subscription: ZuoraSubscription,
      cohortItem: CohortItem,
      date: LocalDate
  ): Option[SubscriptionNotificationAnalyseResult] = {
    // The check here consists in verifying that the product name is "Supporter Plus" [1] and that
    // The billing period of the subscription's active rate plan is the same as the cohort item [2]

    // [1] The first discrepancy happens when the customer has upgraded to DigitalPack
    // [2] The second discrepancy happens when the customer migrated from Monthly to Annual

    for {
      ratePlan <- SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(
        subscription,
        date
      )
      subscriptionBillingPeriod <- ZuoraRatePlan.ratePlanToOptionalUniquelyDeterminedBillingPeriod(ratePlan)
      cohortItemBillingPeriod <- cohortItem.billingPeriod
    } yield {
      if (
        ratePlan.productName == "Supporter Plus" &&
        BillingPeriod.toString(subscriptionBillingPeriod) == cohortItemBillingPeriod
      ) {
        SNARReadyToNotify
      } else {
        SNARExcludeFromMigration
      }
    }
  }

  def analyseSubscriptionForNotification(
      cohortSpec: CohortSpec,
      subscription: ZuoraSubscription,
      cohortItem: CohortItem,
      date: LocalDate,
      ratePlanProbeResult: RatePlanProbeResult
  ): Option[SubscriptionNotificationAnalyseResult] = {
    if (subscription.status == "Cancelled") {
      Some(SNARCancelledInZuora)
    } else if (!NotificationHandlerHelper.thereIsEnoughNotificationLeadTime(cohortSpec, date, cohortItem)) {
      Some(SNARMissingNotificationWindow)
    } else {
      MigrationType(cohortSpec) match {
        case Test1                  => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case GuardianWeekly2025     => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Newspaper2025P1        => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Newspaper2025P3        => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case ProductMigration2025N4 => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Membership2025         => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case DigiSubs2025           => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case SupporterPlus2026 => analyseSubscriptionForNotification_SupporterPlus2026(subscription, cohortItem, date)
        case Print2026C1GWAnnualsUK     => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C1GWQuarterliesUK => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C1NPAnnualsUK     => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C1NPQuarterliesUK => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C1NPSemiannualsUK => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C2NPMonthliesUK   => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C3GWMonthliesUK   => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C3NPMonthliesUK   => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C4NPMonthliesUK   => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C5GW              =>
          analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C5NP =>
          analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
        case Print2026C6GWQuarterliesNonUK => analyseSubscriptionForNotification_Legacy(ratePlanProbeResult)
      }
    }
  }
}
