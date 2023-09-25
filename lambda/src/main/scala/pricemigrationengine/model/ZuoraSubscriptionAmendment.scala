package pricemigrationengine.model

import java.time.LocalDate

import pricemigrationengine.model.OptionReader // not sure why this import is needed as should be visible implicitly
import upickle.default._

/*
{
  "success" : true,
  "id" : "8a129e8f8abcd26f018acc9501e37fbc",
  "code" : "A-AM08077754",
  "name" : "remove",
  "type" : "RemoveProduct",
  "description" : null,
  "status" : "Completed",
  "contractEffectiveDate" : "2023-10-28",
  "serviceActivationDate" : "2023-10-28",
  "customerAcceptanceDate" : "2023-10-28",
  "effectiveDate" : "2023-10-28",
  "suspendDate" : null,
  "resumeDate" : null,
  "newSubscriptionId" : "8a129e8f8abcd26f018acc9502127fc1",
  "baseSubscriptionId" : "8a129e8f8abcd26f018acc9500137f9b",
  "bookingDate" : "2023-09-25",
  "termType" : null,
  "currentTerm" : null,
  "currentTermPeriodType" : null,
  "termStartDate" : null,
  "renewalSetting" : null,
  "renewalTerm" : null,
  "renewalTermPeriodType" : null,
  "autoRenew" : null,
  "specificUpdateDate" : null,
  "newRatePlanId" : "8a129e8f8abcd26f018acc9501cc7fb9",
  "baseRatePlanId" : "8a129e8f8abcd26f018acc9500247fa2",
  "destinationAccountId" : null,
  "destinationInvoiceOwnerId" : null,
  "subType" : null,
  "effectivePolicy" : null,
  "createdByOrder" : false
}
 */

/*
  Date: 17th Sept 2023

  Class ZuoraSubscriptionAmendment was originally introduced to detect whether or not there has been an
  amendment on a subscription between the estimation step and the amendment step.

  The check was introduced to prevent a problem by which, at least for the digital products we have scheduled
  for price rise in 2023, (where people would be notified by email, and where the  subscriptions can be managed online),
  there was also possibility for user to perform user-triggered subscription mutations online (for instance product
  changes), which would not be known to the engine and which would then be lost and/or overridden by the engine at
  amendment step. Pascal then decided that the engine would not proceed with any price rise if there were amendments
  on the subscription between the estimation step and the amendment step.

  This logic solves the immediate problem, but such a simple check is too crude and too inefficient, as it will prevent
  some subscriptions from being migrated if they undergo mutations (amendments) that have nothing to do with user
  journeys. In an ideal world we would retrieve the entire collection of amendments, analyse the recent ones and
  make a determination on whether or not a price rise can happen.

  Unfortunately Zuora, as far as we know, only offers us the retrieval of the last amendment. This is enough for the
  check since we can then compare the date of estimation with the effective date of the amendment (which seems to
  coincide with the date the amendment was created). But, this is clearly not enough for the type of  thorough evaluation
  that would lead to a better check (without false positives).

  1. At the moment we only need the bookingDate. This is the reason why this first version of the class only carries
     that one attribute.

  2. It will be great if in the future the following is done
     - 2.1 Find a way to retrieve more amendments, ideally the entire sequence of amendments of a subscription.
     - 2.2 Implement an analysis of that sequence to provide a better answer to the question: Should the engine perform
         the scheduled price rise on the corresponding subscription.

  Together with ZuoraSubscriptionAmendment we introduce
    - case class IncompatibleAmendmentHistory, and
    - checkMigrationRelevanceBasedOnLastAmendment in the amendment handler
 */

case class ZuoraSubscriptionAmendment(bookingDate: String)

object ZuoraSubscriptionAmendment {
  implicit val rwSubscription: ReadWriter[ZuoraSubscriptionAmendment] = macroRW
}
