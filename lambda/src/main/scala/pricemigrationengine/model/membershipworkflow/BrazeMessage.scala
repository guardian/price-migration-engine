package pricemigrationengine.model.membershipworkflow

import pricemigrationengine.model.OptionWriter //required
import pricemigrationengine.model.OptionReader //required
import upickle.default.{ReadWriter, macroRW}

case class BrazePayloadSubscriberAttributes(
    title: Option[String],
    first_name: String,
    last_name: String,
    billing_address_1: String,
    billing_address_2: Option[String],
    billing_city: Option[String],
    billing_postal_code: String,
    billing_state: Option[String],
    billing_country: String,
    payment_amount: String,
    next_payment_date: String,
    payment_frequency: String,
    subscription_id: String,
    product_type: String,

    // -----------------------------------------------
    // Newspaper2025P1 (extension)
    // (Comment Group: 571dac68)
    newspaper2025_brand_title: Option[String] = None,
    // -----------------------------------------------

    // -----------------------------------------------
    // Newspaper2025P3 (extension)
    newspaper2025_phase3_brand_title: Option[String] = None,
    // -----------------------------------------------

    // -----------------------------------------------
    // ProductMigration2025N4 (extension)
    newspaper2025_phase4_brand_title: Option[String] = None,
    newspaper2025_phase4_formstack_url: Option[String] = None,
    // -----------------------------------------------

    // -----------------------------------------------
    // SupporterPlus2026 (extension)
    sp2026_contribution_amount: Option[String] = None,
    sp2026_current_combined_amount: Option[String] = None,
    sp2026_new_combined_amount: Option[String] = None,
    // -----------------------------------------------
)

object BrazePayloadSubscriberAttributes {
  implicit val rw: ReadWriter[BrazePayloadSubscriberAttributes] = macroRW
}

case class BrazePayloadContactAttributes(SubscriberAttributes: BrazePayloadSubscriberAttributes)

object BrazePayloadContactAttributes {
  implicit val rw: ReadWriter[BrazePayloadContactAttributes] = macroRW
}

case class BrazePayload(Address: Option[String], ContactAttributes: BrazePayloadContactAttributes)

object BrazePayload {
  implicit val rw: ReadWriter[BrazePayload] = macroRW
}

case class BrazeMessage(
    To: BrazePayload,
    DataExtensionName: String,
    SfContactId: String,
    IdentityUserId: Option[String]
)

object BrazeMessage {
  implicit val rw: ReadWriter[BrazeMessage] = macroRW
}
