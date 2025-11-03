package pricemigrationengine.model

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
      case SupporterPlus2024  => true
      case GuardianWeekly2025 => true
      case Newspaper2025P1    => {
        List(
          assertNonTrivialValue(
            "newspaper2025_brand_title",
            message.To.ContactAttributes.SubscriberAttributes.newspaper2025_brand_title
          )
        ).forall(identity)
      }
      case HomeDelivery2025 => {
        List(
          assertNonTrivialValue(
            "homedelivery2025_brand_title",
            message.To.ContactAttributes.SubscriberAttributes.homedelivery2025_brand_title
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
      case Membership2025 => true
    }
  }
}
