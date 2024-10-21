package pricemigrationengine.model

import pricemigrationengine.migrations.GuardianWeeklyMigration
import pricemigrationengine.model.ChargeOverride.fromRatePlanCharge

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import pricemigrationengine.model.Either._
import pricemigrationengine.model.ZuoraProductCatalogue.productPricingMap
import upickle.default.{ReadWriter, macroRW}

/*
{
  "orderDate": "2024-10-21",
  "existingAccountNumber": "A01269270",
  "subscriptions": [
    {
      "subscriptionNumber": "A-S01310226",
      "orderActions": [
        {
          "type": "RenewSubscription",
          "triggerDates": [
            {
              "name": "ContractEffective",
              "triggerDate": "2024-10-21"
            },
            {
              "name": "ServiceActivation",
              "triggerDate": "2024-10-21"
            },
            {
              "name": "CustomerAcceptance",
              "triggerDate": "2024-10-21"
            }
          ]
        }
      ]
    }
  ],
  "processingOptions": {
    "runBilling": false,
    "collectPayment": false
  }
}
 */

case class ZuoraRenewOrderPayloadOrderActionTriggerDate(name: String, triggerDate: LocalDate)
object ZuoraRenewOrderPayloadOrderActionTriggerDate {
  implicit val rwZuoraRenewOrderPayloadOrderActionTriggerDate
      : ReadWriter[ZuoraRenewOrderPayloadOrderActionTriggerDate] = macroRW
}

case class ZuoraRenewOrderPayloadOrderAction(
    `type`: String,
    triggerDates: List[ZuoraRenewOrderPayloadOrderActionTriggerDate]
)
object ZuoraRenewOrderPayloadOrderAction {
  implicit val rwZuoraRenewOrderPayloadOrderAction: ReadWriter[ZuoraRenewOrderPayloadOrderAction] = macroRW
}

case class ZuoraRenewOrderPayloadSubscription(
    subscriptionNumber: String,
    orderActions: List[ZuoraRenewOrderPayloadOrderAction]
)
object ZuoraRenewOrderPayloadSubscription {
  implicit val rwZuoraRenewOrderPayloadSubscription: ReadWriter[ZuoraRenewOrderPayloadSubscription] = macroRW
}

case class ZuoraRenewOrderPayloadProcessingOptions(runBilling: Boolean, collectPayment: Boolean)
object ZuoraRenewOrderPayloadProcessingOptions {
  implicit val rwZuoraRenewOrderPayloadProcessingOptions: ReadWriter[ZuoraRenewOrderPayloadProcessingOptions] = macroRW
}

case class ZuoraRenewOrderPayload(
    orderDate: LocalDate,
    existingAccountNumber: String,
    subscriptions: List[ZuoraRenewOrderPayloadSubscription],
    processingOptions: ZuoraRenewOrderPayloadProcessingOptions
)
object ZuoraRenewOrderPayload {
  implicit val rwZuoraRenewOrderPayload: ReadWriter[ZuoraRenewOrderPayload] = macroRW
}
