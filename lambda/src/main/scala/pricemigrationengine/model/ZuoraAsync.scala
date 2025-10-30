package pricemigrationengine.model

import upickle.default._

// This module was added in August 2025 to solve a problem
// with renew orders and amendments orders, which became prevalent as we
// have started many print migrations this summer.
//
// If the subscription has many amendments (more than 150 of them), for
// instance subscriptions with a large number of holiday stops, a renew
// order or an amendment order can timeout.
//
// Unfortunately when that happens the state of the subscription
// in Zuora is undefined: the operation sometimes succeed a couple of minutes later
// or the operation has completely failed. Because of the indeterminacy of the
// result, it is not recommended to retry, because it may result in the operation
// having succeeded twice, and that without the client ever receiving a 200.

/*

An example of The HTTP request for an async renew is:

curl -X POST \
  "https://rest.zuora.com/v1/async/orders/" \
  -H "Authorization: Bearer [removed]" \
  -H "Content-Type: application/json" \
  -d '{
  "orderDate": "2025-08-12",
  "existingAccountNumber": "[removed]",
  "subscriptions": [
    {
      "subscriptionNumber": "[removed]",
      "orderActions": [
        {
          "type": "RenewSubscription",
          "triggerDates": [
            {
              "name": "ContractEffective",
              "triggerDate": "2025-08-12"
            },
            {
              "name": "ServiceActivation",
              "triggerDate": "2025-08-12"
            },
            {
              "name": "CustomerAcceptance",
              "triggerDate": "2025-08-12"
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
}'

The server then return the job id

{
  "jobId" : "8a129101989d368501989f0e18ec7d98",
  "success" : true
}

The Job id can then be used to pull the status of the job and we need to keep
doing this until we receive the `Completed` status.

curl -X GET \
  "https://rest.zuora.com/v1/async-jobs/8a129101989d368501989f0e18ec7d98" \
  -H "Authorization: Bearer [removed]"

The various statuses are given below:

{
  "status" : "Processing",
  "errors" : null,
  "result" : null,
  "success" : true
}

{
  "status" : "Failed",
  "errors" : "[40000050]: Operation failed due to a lock competition, please retry later.",
  "result" : null,
  "success" : true
}

{
  "status" : "Failed",
  "errors" : "[40000060]: Oops, internal error occurred, please contact Zuora support.",
  "result" : null,
  "success" : true
}

{
    "Build": "2806",
    "CohortName": "Membership2025",
    "INFO": "[4b4e379c] jobReport: AsyncJobReport(Failed,Some([40000020]: Rate plan \"Non Founder Supporter - monthly\" is not effective on the contract effective date of 03/12/2025.))"
}

{
  "status" : "Completed",
  "errors" : null,
  "result" : {
    "orderNumber" : "O-04857851",
    "accountNumber" : "[removed]",
    "status" : "Completed",
    "subscriptionNumbers" : [ "[removed]" ],
    "jobType" : "AsyncCreateOrder"
  },
  "success" : true
}

The above process is identical for subscription amendment (meaning product migration)
orders

 */

case class AsyncJobSubmissionTicket(jobId: String, success: Boolean)
object AsyncJobSubmissionTicket {
  implicit val reader: Reader[AsyncJobSubmissionTicket] = macroR
}

case class AsyncJobReport(status: String, errors: Option[String])
object AsyncJobReport {
  implicit val reader: Reader[AsyncJobReport] = macroR
  def isReady(report: AsyncJobReport): Boolean = report.status == "Completed"
  def hasFailed(report: AsyncJobReport): Boolean = report.status == "Failed"
}
