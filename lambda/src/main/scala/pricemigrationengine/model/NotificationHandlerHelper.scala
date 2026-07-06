package pricemigrationengine.model

import java.time.LocalDate
import pricemigrationengine.model.membershipworkflow.EmailMessage

object NotificationHandlerHelper {

  def assertNonTrivialValue(fieldName: String, value: Option[String]): Boolean = {
    value.isDefined && value.get.nonEmpty
  }

  def messageIsWellFormed(cohortSpec: CohortSpec, message: EmailMessage): Boolean = {
    // This function return whether or not an EmailMessage is "well formed". And for the moment
    // this is limited to checking that the special circumstances extra attributes (which were
    // originally introduced for the Summer 2025 print migrations) are not empty.

    MigrationType(cohortSpec) match {
      case Test1              => true
      case GuardianWeekly2025 => true
      case Newspaper2025P1    => {
        List(
          assertNonTrivialValue(
            "newspaper2025_brand_title",
            message.To.ContactAttributes.SubscriberAttributes.newspaper2025_brand_title
          )
        ).forall(identity)
      }
      case Newspaper2025P3 => {
        List(
          assertNonTrivialValue(
            "newspaper2025_phase3_brand_title",
            message.To.ContactAttributes.SubscriberAttributes.newspaper2025_phase3_brand_title
          )
        ).forall(identity)
      }
      case ProductMigration2025N4 => {
        List(
          assertNonTrivialValue(
            "newspaper2025_phase4_brand_title",
            message.To.ContactAttributes.SubscriberAttributes.newspaper2025_phase4_brand_title
          ),
          assertNonTrivialValue(
            "newspaper2025_phase4_formstack_url",
            message.To.ContactAttributes.SubscriberAttributes.newspaper2025_phase4_formstack_url
          ),
        ).forall(identity)
      }
      case Membership2025    => true
      case DigiSubs2025      => true
      case SupporterPlus2026 => true
    }
  }

  def expectedProductNameOpt(cohortSpec: CohortSpec): Option[String] = {
    MigrationType(cohortSpec) match {
      case Test1                  => None
      case GuardianWeekly2025     => None
      case Newspaper2025P1        => None
      case Newspaper2025P3        => None
      case ProductMigration2025N4 => None
      case Membership2025         => None
      case DigiSubs2025           => None
      case SupporterPlus2026      => Some("Supporter Plus")
    }
  }

  def checkProductName(
      ratePlan: ZuoraRatePlan,
      today: LocalDate,
      productNameOpt: Option[String]
  ): Boolean = {
    // This function essentially returns `true` if the rate plan product name is
    // what we expect. This was introduced to ensure that at Notification time
    // the subscription has not moved to a different product. This can happen to,
    // for instance, to Supporter Plus subs that can be transmuted to Digital Packs

    productNameOpt match {
      case Some(productName) => {
        ratePlan.productName == productName
      }
      case None => true // for backward compatibility when the information is not available for previous subs
    }
  }
}
