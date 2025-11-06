## The journey of a cohort item

In [Price migrations from first principles](./price-migrations-from-first-principles.md) we have seen what price migrations are, and the general logic behind then. In this chapter let's have a closer look at the actual steps of a price migration following the journey of a cohort item in the engine.

### What is a cohort item ?

In the engine parlance a "cohort" is a set of subscription numbers that are part of a given price migration. Logically a "cohort item" would then be one of those numbers, but in fact it refers to a record in the Dynamo table where the engine maintains information about the price migration.

One such cohort item seen in one of the Dynamo tables is shown below

![](./the-journey-of-a-cohort-item/1707822328.png)

The price migration may apply to a collection of subscriptions, but each subscription is price risen independently to the others (and yes, a price migration could just have one subscription in it), so to understand the logic of a migration we only need to follow the journey of one given subscription number.

### Loading the subscription numbers and creating the Dynamo table.

A price migration fundamentally need two ingredients:

1. The engine has been updated with the required code to encode the specificities of the migration (for instance specific features we implemented for Newspaper2024)

2. Marketing has provided us with the list of subscription numbers of all the subscriptions that are part of the migration. That list comes as a file with one subscription number per line, like this

```
S-00000001
S-00000002
S-00000003
S-00000004
etc..
``` 

At this point we need to have decided a name for the migration. Let's imagine we called it Newspaper2024. We locate the `price-migration-engine-prod` S3 bucket and create a directory called `Newspaper2024` in which we put a file called `subscription-numbers.csv`.

The engine will not automagically pick up the file, it needs to be instructed to do so (this will not be covered here), but the effect of the Dynamo table being created and the file having been loaded is that we now have a table with records and each record simply contains a subscription number.

To help with understanding of the following steps let's use a (simplified) version of the CohortItem case class in the engine Scala Code

```
case class CohortItem(
    subscriptionName : String,
    processingStage  : String,
    currency         : Option[String]     = None,
    billingPeriod    : Option[String]     = None,
    oldPrice         : Option[BigDecimal] = None,
    estimatedNewPrice: Option[BigDecimal] = None,
    amendmentEffectiveDate : Option[LocalDate]  = None,
    newPrice         : Option[BigDecimal] = None,
)
```

So, when the subscription number is loaded into the table, the cohort item is essentially 

```
CohortItem(
    subscriptionName = "S-00000003"
)
```

With that said we need to indicate the processing stage an in that case it will be "ReadyForEstimation", so the cohort item is actually 

```
CohortItem(
    subscriptionName = "S-00000003"
    processingStage  = "ReadyForEstimation"
)
```

In this stage the engine essentially is saying "I have recorded this subscription number and it's ready to be Estimated"

### The Estimation Stage

The estimation stage has several purposes, in fact 3 main purposes 

1. Looking up metadata about the subscription in Zuora
2. Deciding what the post migration price is going to be
3. Deciding the start date, meaning the future billing date that the price rise is going apply.

Considering our subscription number `S-00000003`, let's imagine that the subscription look up in Zuora reveal the following information: currency: `EUR`, billing period: `Monthly`, old price: `52`. The cohort item then becomes

```
CohortItem(
    subscriptionName = "S-00000003"
    processingStage  = (...)
    currency         = Some("EUR")
    billingPeriod    = Some("Monthly")
    oldPrice         = Some(BigDecimal(52))
)
```

Let's also assume that we know that the most migration price for this subscription (considering the type of product it carries) is `61`. In the case of the Newspaper2024 migration, we knew the post migration price of any of the products because it was actually hardcoded in the engine code (or computed using the `ChargeDistribution2024` algebra we introduced). The cohort items becomes 

```
CohortItem(
    subscriptionName  = "S-00000003"
    processingStage   = (...)
    currency          = Some("EUR")
    billingPeriod     = Some("Monthly")
    oldPrice          = Some(BigDecimal(52))
    estimatedNewPrice = Some(BigDecimal(61))
)
```

where "estimatedNewPrice" refers to the price post-rise. 

The last bit of data is the billing date that the price rise should be applied to. If you remember the explanations in Chapter 1, we saw that this date is computed considering a collection of factors (how old the subscription is, the billing period, how close the next natural billing period would be to the earliest possible time for notification and the need to wait at least 30 days etc). Let's imagine that the computed start date is `2024-05-10`. The cohort item becomes

```
CohortItem(
    subscriptionName  = "S-00000003"
    processingStage   = (...)
    currency          = Some("EUR")
    billingPeriod     = Some("Monthly")
    oldPrice          = Some(BigDecimal(52))
    estimatedNewPrice = Some(BigDecimal(61))
    amendmentEffectiveDate = Some(LocalDate.of(2024, 5, 10))
)
```

At this stage the cohort items has been "estimated". Technically the cohort item is now in processing stage `EstimationComplete`. With that said, the engine is immediately going to create a record in Salesforce to mark the subscription in Salesforce with the new post migration price. At this point the processing stage is now `SalesforcePriceRiceCreationComplete`.

```
CohortItem(
    subscriptionName  = "S-00000003"
    processingStage   = "SalesforcePriceRiceCreationComplete"
    currency          = Some("EUR")
    billingPeriod     = Some("Monthly")
    oldPrice          = Some(BigDecimal(52))
    estimatedNewPrice = Some(BigDecimal(61))
    amendmentEffectiveDate = Some(LocalDate.of(2024, 5, 10))
)
```

Then, the cohort item is going to.... sleep. It's going to sleep as long as it take for it to be at the start date minus about 40 days. This might take a few days or up to a year.

### Notification 

Today is now `LocalDate.of(2024, 5, 10)` minus about 40 days. The engine sees a subscription in `SalesforcePriceRiceCreationComplete` stage ready to be user notified. The engine is going to perform a certain number of lookups and will send a message to a queue that will eventually be delivered to Braze (triggering the sending of an email, or sending an additional request to an external company for a letter to be printed and delivered).

The message to the user is going to mention the start date and the new estimated new price. The outcome of this step is the subscription being put in processing stage `NotificationSendComplete`.

Once the item is in `NotificationSendComplete` stage the SalesforceNotificationDateUpdatehandler lambda will fire up. This operation notifies Salesforce that the user has been notified (for record keeping) and puts the item in processing stage `NotificationSendDateWrittenToSalesforce`.

### Amendment

The item now being in processing stage `NotificationSendDateWrittenToSalesforce` the engine will perform an amendment in Zuora, meaning will update Zuora with the fact that the subscription in Zuora is price risen and that the price rise is taking effect on the date that had already been decided during the Estimation step. Note that this is the first and only moment that the engine performs a write operation in Zuora during the entire price rise process of that subscription and puts the item in `AmendmentComplete`.

The next step is yet another Salesforce update, where we inform Salesforce that the subscription in Zuora has been edited for price rise. Then the processing stage becomes `AmendmentWrittenToSalesforce`. This completes the price rise of subscription "S-00000003".

It is also important to notice that the amendment step only happened after the user notification step. This is a security to avoid a situation where there would be a bug in the engine or even just a long outage, causing subscriptions to be price risen in Zuora without the users having (yet) been notified. That would be illegal and put the Guardian in hot water.

Last, but not least, this entire section, eg: moving through 

- `SalesforcePriceRiceCreationComplete`
- `NotificationSendComplete`
- `NotificationSendDateWrittenToSalesforce`
- `AmendmentComplete`
- `AmendmentWrittenToSalesforce`

happens the same day. Being independent steps it is possible to delay the next one of the sequence, but there is also value in letting in them complete the same day, so that is a customer calls a CRS, they see, in Zuora and Salesforce, an up-to-date view of what the engine was in the process of doing.

### Zuora Cancellations

As we have seen a cohort item / subscription can sleep for a long time before it is ready to move to notification process, but what happens if the subscription has been cancelled by the user in the meantime ?

In such a case, the engine will detect that the subscription has been cancelled in Zuora and will move the cohort item to `ZuoraCancellation` processing stage

```
CohortItem(
    subscriptionName  = "S-00000003"
    processingStage   = "ZuoraCancellation"
)
```

Once the cohort item is in `ZuoraCancellation` state the engine will no longer touch it. Alike `AmendmentWrittenToSalesforce`, `ZuoraCancellation` is a final state for a cohort item.

### Non standard processing stages

There are two types of non standard processing stages. They all express that the migration of the cohort item, did not 
complete for another reason than a cancellation in Zuora. The first type are those generated by the engine itself, and 
the second type are essentially issued by Pascal.

Type 1 (generated by the engine)

- `NoPriceIncrease`: The cohort item is put in this state if the price after estimation is equal to or lower than the current price of the subscription
- `EstimationStepEmptyZuoraInvoicePreview`: This state corresponds to when during estimation Zuora returned an empty invoice preview. 
  It is a terminal state as far as the engine is concerned but might not be the end of the journey of the subscription (see next sections for details).

Type 2 (generated by Pascal)

- `ExcludedFromMigration`: This state results from special requests from marketing, and sometime comes in variants, for 
  instance GuardianWeekly2025 has elements in state `ExcludedFromMigration-WrongCountry` (after Marketing realised that 
  too many countries had been originally added to the list of subscription numbers as part of the initialisation of the  
  migration with). They are always generated manually and never originate from the engine.

### So, what's the deal with EstimationStepEmptyZuoraInvoicePreview ?

`EstimationStepEmptyZuoraInvoicePreview` is an extremely rare state, for instance we had 19 of them for SupporterPlus2024 
which had 110,000 subscriptions. And no more than one for the print migrations. (For the print products Zuora was timing out, 
but that's a different problem and it's been solved.)

In any case, if Zuora was the type of system to be trusted (spoiler alert, it's not!), this state would be terminal. But we 
observed that for some subscriptions Zuora did return an empty invoice during Estimation, but then returned a full invoice when 
we tried a few days later 🙃

As a result, the operational directive with this state is that it's ok not to worry about it, but if the engine steward(s) 
observe it, then it's also ok to double check them. If the sub has a confirmed empty invoice preview (for instance because 
the rate plans are expired but the subscription was not cancelled), then it can manually be moved to `ExcludedFromMigration`, 
or better, cancelled and then manually moved (back) to `ZuoraCancellation`. If on the other hand it was a mis-classification 
then the item should be put in `ReadyForEstimation`. Consequently, the ideal situation is a migration 
without `EstimationStepEmptyZuoraInvoicePreview` which indicates that either it never happened or any occurrence of it 
was double-checked by the steward(s).

Note that there was another way to deal with `EstimationStepEmptyZuoraInvoicePreview`, which would simply be to teach 
the engine to retry those items later (for instance a day), but then we would need to encode a counter so that it 
gives up after a certain number of days, but Pascal estimated that this full automation, for an event very rare wasn't 
worth the (heavy) extra cost in the design of the estimation Handler.

### Processing Lambdas

The price migration engine define a state machine which is linear. The lambdas fire in a given linear order (occasionally 
the same lambda fires more than once) For reference here is the other in which the lambda fire 

- CohortTableCreationHandler
- SubscriptionIdUploadHandler
- EstimationHandler
- SalesforcePriceRiseCreationHandler
- NotificationHandler
- SalesforceNotificationDateUpdateHandler
- AmendmentHandler
- SalesforceAmendmentUpdateHandler
