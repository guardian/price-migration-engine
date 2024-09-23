package pricemigrationengine.model.membershipworkflow

import pricemigrationengine.model.OptionWriter //required
import pricemigrationengine.model.OptionReader //required
import upickle.default.{ReadWriter, macroRW}

case class EmailPayloadSubscriberAttributes(
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

    // SupporterPlus 2024 extension (see comment below)
    sp2024_contribution_amount: Option[String] = None,
    sp2024_previous_combined_amount: Option[String] = None,
    sp2024_new_combined_amount: Option[String] = None,
)

/*
    Date: September 2024
    Author: Pascal
    Comment Group: 602514a6-5e53

    This note describes an extension of the EmailPayloadSubscriberAttributes that we are introducing specifically
    for the SupporterPlus 2024 migration.

    So far in the history of the engine, we have communicated to the customers the new (post increase) price of their
    subscription. This information is carried by payment_amount.

    The SupporterPlus migration is making a price increase on subscriptions which have a base price and an optional
    contribution that is at the discretion of the user. For instance some monthly user pay £[10, 0], which means that
    the user pays the base price of £10 and no extra contribution, whereas another user would pay £[10, 15], which means
    that the user pays the base price of £10 and an extra contribution of £15, leading to a total price of £25.

    In the SupporterPlus2024 migration we are increasing the base price only, for GBP moving from 10 to 12.

    We want to communicate to the user that only the base price is being price increased and not their extra
    contribution. More exactly for customers paying no extra contribution we want to say

    """
    Your new price will take effect at your next payment date, on or around {next_payment_date}, and your new payment amount will be {payment_amount} - {payment_frequency}.
    """

    And for customers with an extra contribution with want to say

    """
    Your new subscription price will take effect at your next payment date, on or around {Next Payment Date}.
    Your {payment_frequency} contribution of {contribution_amount} remains unchanged, and so your total billing amount will now be {new_combined_amount} - {payment_frequency}.
    """

    We are introducing three new fields

    sp2024_contribution_amount
    sp2024_previous_combined_amount
    sp2024_new_combined_amount

    Those fields are going to be used by the emails templates that we are going to use for S+ 2024 and
    should not be used for any other migrations.

    I am going to decommission them a bit after October 2025, after the end of the migration. I will
    nevertheless permanently document this modification in case it is needed again in some remote future
    if/when we decide to make another SupporterPlus price migration.
 */

object EmailPayloadSubscriberAttributes {
  implicit val rw: ReadWriter[EmailPayloadSubscriberAttributes] = macroRW
}

case class EmailPayloadContactAttributes(SubscriberAttributes: EmailPayloadSubscriberAttributes)

object EmailPayloadContactAttributes {
  implicit val rw: ReadWriter[EmailPayloadContactAttributes] = macroRW
}

case class EmailPayload(Address: Option[String], ContactAttributes: EmailPayloadContactAttributes)

object EmailPayload {
  implicit val rw: ReadWriter[EmailPayload] = macroRW
}

case class EmailMessage(
    To: EmailPayload,
    DataExtensionName: String,
    SfContactId: String,
    IdentityUserId: Option[String]
)

object EmailMessage {
  implicit val rw: ReadWriter[EmailMessage] = macroRW
}
