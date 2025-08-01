# Price Migration Engine - Lambdas

## Development

### Code structure

The codebase is structured to keep a clear separation
between [pure](https://docs.scala-lang.org/overviews/scala-book/pure-functions.html)
and [effectful](https://en.wikipedia.org/wiki/Side_effect_(computer_science)) code.  
(*Effectful* has different meanings, but I mean by it having side-effects or not pure.)

The [model](src/main/scala/pricemigrationengine/model) package holds all the pure code in the project. All code here is
deterministic. It neither generates nor depends on any kind of real-world effect; including random numbers, relative
dates, logging or printing to console.

The effects of the code are generated and consumed through [services](src/main/scala/pricemigrationengine/services),
following the [ZIO convention](https://zio.dev/docs/overview/overview_testing_effects#environmental-effects). Each
service has a type, an interface and at least one implementation. For example,
the [Zuora type](src/main/scala/pricemigrationengine/services/package.scala),
[service definition](src/main/scala/pricemigrationengine/services/Zuora.scala) and
[live implementation](src/main/scala/pricemigrationengine/services/ZuoraLive.scala).

These services are composed together into ZIO vertical and
horizontal [layers](https://github.com/zio/zio/blob/master/docs/datatypes/zlayer.md), and these layers form the runtime
environment for each of the lambdas by compile-time dependency injection. A vertical layer is one in which one service
depends on another:
they are related together by the `>>>` operator. In a horizontal layer, two peer services are related together by
the `++` operator.  
For a more detailed explanation of how these layers work, see
the [ZIO documentation](https://zio.dev/docs/howto/howto_use_layers).

The lambdas are all in the [handlers](src/main/scala/pricemigrationengine/handlers) package.

All the [dependencies](../project/Dependencies.scala) of the project have been chosen for their light weight and minimal
number of transitive dependencies, so that the artefact generated is of minimal size and lambdas can warm up quickly.

The same generated jar is used by all the lambdas. The only variation in their deployment is the configuration of the
main endpoint.

# Workflow

The process of implementing price rises has a number of discrete steps, each being implemented by a separate lambda.

The steps are primarily co-ordinated via the dynamoDB 'CohortTable' which contains an item for each subscription that is
put through the price rise process.

When each lambda is executed it will select items from the CohortTable which have a 'processingStage' of a particular
value.

Once complete they set the 'processingStage' to a value that indicates that stage has been executed and they are ready
to be pick up by the next stage.

For the most part the lambdas are idempotent, so if a stage fails at any point it can be re-run and it will reprocess
items in the CohortTable that were not completely processed in the failed execution.

In some cases lambdas are not idempotent, eg the NotificationHandler which has the potential to send multiple direct
messages to customers for the same subscription. This lambda sets the status of the CohortItem to a
'processing' status before sending the notification and sets it to a 'complete' status once the notification has been
sent successfully. If the notification send fails the cohort item stay in the 'processing' state which will require
manual intervention to put it into a state where it would be re-processed or made available to the next stage for
subsequent processing.

The stages are as follows

| Processing stage at start | Lambda | Description | Processing stage on completion |
|---------|---|---|---|
| N/A | SubscriptionIdUploadHandler | Initialises the items in the CohortTable for more details see:See [importSubscriptionId.md](./docs/importSubscriptionId.md) | ReadyForEstimation |
| ReadyForEstimation | EstimationHandler | Uses Zuora to 'estimate' new price and start date of price rise | EstimationComplete |
| EstimationComplete | SalesforcePriceRiseCreationHandler | Creates the prices rise object in SF so the estimated information is available to CSRs | SalesforcePriceRiceCreationComplete/Cancelled (if cancellation detected) |
| SalesforcePriceRiceCreationComplete | NotificationHandler | Sends prices rise notification direct notification to customer via braze | NotificationSendProcessing (on failure)/NotificationSendComplete (on success) |
| NotificationSendComplete | AmendmentHandler | Applies the prices rise amendment to Zuora | AmendmentComplete/Cancelled (if cancellation detected) |

### EstimationHandler

#### Generating the start date, i.e.: the day the new rate plan/price-rise comes into effect for a subscription 

The cohort as a whole has a start date, which is set in the cohort spec. So for any individual sub, the price rise (i.e.: the date the new Zuora ratePlan becomes active) will take effect on that date or later. For any sub the price rise date will be the next service period start date on or later than that overall start date.

1. We spread the start dates out randomly for each **monthly** subscription [over 3 months](./src/main/scala/pricemigrationengine/handlers/EstimationHandler.scala#L78-L80). 

We spread the dates out to reduce impact on our CSR's, because quite a few people cancel because of the price-rise, so if we spread it out we can reduce traffic of people calling our CSR's to cancel their subscription (you can't cancel subscriptions through manage.theguardian.com so customers are forced to phone in)

2. We make sure the startDate of the new rate plan matches the start date of a future invoice. E.g.: [here is a test that generates the new startDate for a subscription:](./src/test/scala/pricemigrationengine/model/AmendmentDataTest.scala#L20-L26), it ensures the startDate is equal to the [serviceStartDate property on this invoice object:](./src/test/resources/Migrations/NewspaperDelivery/Everyday+/InvoicePreview.json#L46-L66)

### To run lambdas locally in Intellij

You can run or debug any of the lambdas in any deployment environment from Intellij.  
You will need up-to-date AWS credentials stored locally.  
Set up a run configuration for the lambda, using the following environment variables:

* AWS_PROFILE=`profileName`
* stage=`DEV|CODE|PROD`

and also the specific environment variables for the lambda you are running.

### Environment variables

The environment variables set in the lambdas are provided by [Secrets Manager](https://eu-west-1.console.aws.amazon.com/secretsmanager/secret?name=price-migration-engine-lambda-PROD&region=eu-west-1) and can also be changed manually in the AWS console by navigating to the lambda (e.g.: price-migration-engine-estimation-lambda-CODE) > Configuration > Environment variables

_Note:_ The DEV and CODE environments both currently point to `apisandbox.zuora.com`. 

We have a copy of our production Zuora environment, the "centralsandbox" (which we access via `test.zuora.com`). However, as we do not have an equivalent salesforce environment set up, we cannot run the step function from start to finish with this environment. 

Individual lambdas like the `EstimationLambda` and the `AmendmentLambda` can be run against this Zuora environment if we want to test on subscriptions in production. See how to [run the lambdas locally](#to-run-lambdas-locally-in-intellij) (see above) with the necessary environment variables.

#### Specific environment variables per lambda

##### EstimationHandler and Amendment Handler

* input=`The input cohort spec in JSON string format` (see below for example)
* earliestStartDate=`earliestStartDate`
* batchSize=`batchSize`
* zuoraApiHost=`host`
* zuoraClientId=`personal clientId`
* zuoraClientSecret=`personal clientSecret`

##### Input Cohort Spec Example
``` 
{
  "cohortName": "EchoLegacyTesting",
  "brazeName": "cmp123",
  "earliestPriceMigrationStartDate": "2022-08-02"
}
```

Note the above is different to the input JSON that the lambda admits 

## Importing subscription id for a price migration

See [importSubscriptionId.md](./docs/importSubscriptionId.md)

## Configuration

Config
======

The configuration for this application is stored in
the [aws secret manager](https://docs.aws.amazon.com/secretsmanager/latest/userguide/intro.html).

The configuration can be updated using the aws console as follows:

- You can either update the secrets using the aws console
    - Log into the aws console using the 'membership' profile via [janus](https://janus.gutools.co.uk/)
    - Navigate to Services > Security, Identity, & Compliance > Secrets Manager
    - Search for price-migration-engine-<STAGE> and click on the result
    - Click 'Retrieve Secret Value' button
    - Click 'Edit'
    - Add/Edit the key value pairs
    - Click 'Save'
- Or use the AWS CLI
    - Get the 'membership' aws credentials from [janus](https://janus.gutools.co.uk/) and add them to your local
      environment
    - Get the existing configuration
    - get the current secrets
      ```bash
      aws --region eu-west-1 --profile membership secretsmanager get-secret-value --version-stage AWSCURRENT --secret-id price-migration-engine-lambda-<STAGE>
      ```
    - The existing string is returned in the "SecretString" element of the json response, you will need to json string
      unescape this value, and then make the changes/additions to the result
    - Create a new version of the secrets with the new secret string:
      ```$bash
      aws --region eu-west-1 --profile membership secretsmanager update-secret --secret-id price-migration-engine-lambda-<STAGE> --secret-string  '{"zuoraApiHost":"http://rest.apisandbox.zuora.com","zuoraClientId":"xxx","zuoraClientSecret":"xxx"}'
      ```      
- Update the secret version in the cloudformation templates. The cloudformation templates contains mappings for the
  version of the secrets in each environment. The new version of the configuration will not be used until those mappings
  are updated. You can do that as follows:
    - Get the latest secret values using the aws cli:
      ```bash
      aws --region eu-west-1 --profile membership secretsmanager get-secret-value --version-stage AWSCURRENT --secret-id price-migration-engine-lambda-<STAGE>
      ```
    - Take the UUID from the value of the "VersionId" field in the response from the above.
    - Update the Mappings > StageMap > <Stage> > SecretsVersion field in this
      projects [cloudformation template](cfn.yaml)
    - Build and deploy the changes using riffraff

## Billing Address Format

The notification letters initiated by the NotificationHandler gets contact details including the billing address from
salesforce.

The salesforce data model is as follows:

```json
{
  "street": "90 York Way",
  "city": "London",
  "country": "United Kingdom",
  "postalCode": "N1 9GU",
  "state": null
}
```

However Zuora and the subscription form have both addressLine1 and addressLine2 (optional). These two fields are
concatenated together when they are synced over from Zuora to Salesforce.

Using addressLine1 and addressLine2 for the direct messages would be preferable, however the issue was discovered too
late in the day to resolve it.

In an attempt to make it simpler to resolve this in the future we are sending billing_address_2 all the way though
sqs/membership-workflow/braze/latcham but we are populating it with an empty string in the NotificationHandler.

## Datalake Export

The data for each cohort is exported each day to the data lake. See [DatalakeExport.MD](DatalakeExport.MD) for more
details.

## How are direct mail letters sent?

- `NotificationHandler` lambda sends SQS message to contribution-thanks for pickup
  by [membership-workflow](https://github.com/guardian/membership-workflow)
- [membership-workflow](https://github.com/guardian/membership-workflow) processes the queue and sends message to API
  triggered campaign in Braze (for example, SV_VO_Pricerise_Q22020)
- The Braze campaign has
  a [webhook](https://www.braze.com/docs/user_guide/message_building_by_channel/webhooks/creating_a_webhook/) configured
  which sends a message to `braze.latchamdirect.co.uk:9090/api/Braze`. [Latcham](https://latcham.co.uk/) is the company
  that prints and mails these letters.
- Note
  the [retry](https://www.braze.com/docs/user_guide/message_building_by_channel/webhooks/creating_a_webhook/#errors-retry-logic-and-timeouts)
  logic of the webhook
- Beware that Latcham has a capacity of 1000 messages per minute, whilst Braze has 50K per second
- Minimal webhook logs can be found in Braze `Developer Console` under Activity Log and filtered by `Webhook Errors`

## Cleaning test data

Subscriptions and InvoicePreviews from the Zuora API can contain sensitive data. We are therefore
using [FixtureSubscriptionCleaner.scala](./src/test/scala/pricemigrationengine/FixtureSubscriptionCleaner.scala)
and [FixtureInvoiceCleaner.scala](./src/test/scala/pricemigrationengine) to search and replace this data. They accept the file path within the resources folder as an argument, e.g.: `Migrations/NewspaperDelivery/Everyday/Subscription.json`. There is no automated script to run these over the whole resources folder, so they must be run on each new file, the result will then be printed to the console and need to be manually copied into file. 

## Inspecting and debugging processed subscriptions

It's useful to group subscriptions in the cohort by their processing stage. From the DynamoDB dashboard, click on Explore Items in the navigation and navigate to the cohort table. Click on the `Scan/Query Items` dropdown, click on `Query`, select `ProcessingStageIndexV2` from the dropdown and then enter a processingStage:

![](../read-me-images/Screenshot 2022-05-10 at 09.40.05.png)
