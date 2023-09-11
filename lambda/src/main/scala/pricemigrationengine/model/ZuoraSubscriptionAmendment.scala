package pricemigrationengine.model

import java.time.LocalDate

import pricemigrationengine.model.OptionReader // not sure why this import is needed as should be visible implicitly
import upickle.default._

/*
{
  "success": true,
  "id": "402896a9529c100a01529c30e26a0018",
  "code": "A-AM00000002",
  "name": "testAmendment",
  "type": "TermsAndConditions",
  "description": "just for test",
  "status": "Completed",
  "contractEffectiveDate": "2016-02-01",
  "serviceActivationDate": "2016-02-01",
  "customerAcceptanceDate": "2016-02-01",
  "effectiveDate": "2016-02-01",
  "newSubscriptionId": "402896a9529c100a01529c311930001d",
  "baseSubscriptionId": "402896a9529c100a01529c2f2cc30010",
  "termType": "TERMED",
  "currentTerm": 2,
  "currentTermPeriodType": "Month",
  "termStartDate": "2015-12-31",
  "renewalSetting": "RENEW_WITH_SPECIFIC_TERM",
  "renewalTerm": 1,
  "renewalTermPeriodType": "Month",
  "autoRenew": false,
  "specificUpdateDate": null,
  "newRatePlanId": null,
  "baseRatePlanId": null,
  "destinationAccountId": "402896a9529bc3dc01529bcba7120023",
  "destinationInvoiceOwnerId": "402896a9529bc3dc01529bcba7120023"
}
 */

case class ZuoraSubscriptionAmendment(
    code: String,
    name: String,
)

object ZuoraSubscriptionAmendment {
  implicit val rwSubscription: ReadWriter[ZuoraSubscriptionAmendment] = macroRW
}
