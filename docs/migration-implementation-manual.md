# The migration implementation manual

In this chapter, we explain how to implement a migration from scratch.

## Introduction

In the chapter [Price migrations from first principles](https://github.com/guardian/price-migration-engine/blob/main/docs/price-migrations-from-first-principles.md) we have discovered why some commercial entities run price migrations. In this chapter I will be describing how we implement them at the Guardian. This description is almost entirely based on Pascal's experience in implementing and monitoring them, but if one day you are responsible for an implementation you may find easier to do things slightly differently.

Price migrations are not P&E projects. They are Marketing led projects. Marketing, in collaboration with Finance, will announce that a collection of subscriptions in Zuora, often corresponding to all subscriptions of a particular product (for instance, Guardian Weekly subscriptions) will need to be migrated. The price migration engine orchestrates that operation from a systems point of view, but this happens within the larger context of operations ran by Marketing.

## The membership-workflow update

A PR needs to be made to register in membership-workflow the name of the campaign (or canvas) the migration will use for user communication. An example (for Newspaper2024) can be found [here](https://github.com/guardian/membership-workflow/pull/505).

## Cohort Specs

As part of setting up a migration you will want to run some of the lambdas on demand in the AWs console. For this you will need a cohort specs object, which is used as input. Here are the two versions of cohort specs for GW2024

```
{
    "cohortName":"GW2024",
    "brazeName":"SV_GW_PriceRise2024Email",
    "earliestAmendmentEffectDate":"2024-05-20"
}

{
    "cohortSpec": {
        "cohortName":"GW2024",
        "brazeName":"SV_GW_PriceRise2024Email",
        "earliestAmendmentEffectDate":"2024-05-20"
    }
}
```

The difference between the two is that the former is used to run specific lambdas and the latter used to run the state machine itself. They both carry the same information.

* **cohortName**: A unique name to identify the cohort. Must consist of alphanumeric, '-' and '_' characters (without space(s)). [1]
* **brazeName**: The name that membership-workflow uses to refer to the Braze campaign or canvas for notifying subscribers. Must consist of alphanumeric, whitespace, '-' and '_' characters. This name is given to us by Marketing after they have set up the campaign or canvas in Braze.
* **earliestAmendmentEffectDate**: Earliest date on which a subscription can have its price increased, or will move to another rate plan (with or without price increase). Increases will always begin on the first day of a billing period on or after this date. Format is `yyyy-mm-dd`.

[1] Pascal always sticks to alphanumerical names, for instance "HomeDelivery2025" (where the year the migration has started appears in the name)

## Subscription numbers upload to the DynamoDB tables

Add a folder to the `price-migration-engine-prod` S3 bucket with the same name as the cohort in the cohort specification (`cohortName` value)

Add the following CSV file to the folder:

* **subscription-numbers.csv**: A file holding line-separated subscription numbers for the price rise, which will typically be the result of a Salesforce report. More info in [subscription-numbers-upload.md](./subscription-numbers-upload.md) on this.

## Coding a migration. High level view

In principle migrations are fundamentally very easy to set up. You will be given a list of subscription ids to load into the engine (this happens by putting a file into a specific location in S3), then you will make a PR to membership-workflow to register the Braze campaign or canvas for the user notifications, and then you just need to add a new record to price-migration-engine-cohort-spec-PROD to ensure that the state machine runs every day with the migration parameters. The engine is fundamentally fully automatic and will operate by itself.

In practice, things are a bit more subtle. To be able to better serve the needs of Marketing, we moved from the idea of extremelly generic code that would apply to all migrations, to treating each migration as a separate event in the engine. The advantage is that it's now very easy to implement migration specific requests. The slight disadvantage is that a new migration requires a PR to be made against the engine code. Luckily we never run more than 3 or 4 migrations each year and if you know what you are doing you can encode them in a day.

In practice the mechanism we use to provide migration specific behavior and data is the `MigrationType` trait. In the current design of the engine the first step in starting a migration in to define a new instance of that trait.

## Data Regularity Checks

Zuora has a rather complex data model, and when coding a migration you might need, or want, to make assumptions about the way the subscriptions and rate plans present themselves. Sometimes those assumptions are essentially incorrect as not enforced by the Zuora data model itself, but happen to be true for the cohort you are working with, and greatly simplify the encoding of that migration. There are no general rules on how to run those checks, because they depend on the specific migration.

One such check, if we had thought about it at the time, would have been checking the date alignement of SupporterPlus 2024 subscriptions. A misalignment was reported (soon before the end of the migration) and led to this PR [https://github.com/guardian/price-migration-engine/pull/1214](https://github.com/guardian/price-migration-engine/pull/1214).

## Migration Steps

We used the Guardian Weekly 2024 migration as a teaching opportunity and split the implementation in 7 Steps. The PR description should help understand what each step acheived and why. 

1. [Extends MigrationType](https://github.com/guardian/price-migration-engine/pull/1012)
2. [Prevent accidental lambda runs](https://github.com/guardian/price-migration-engine/pull/1016)
3. [Test fixtures ](https://github.com/guardian/price-migration-engine/pull/1018)
4. 
    - [The Estimation Step](https://github.com/guardian/price-migration-engine/pull/1019)
    - [Better determination of migration rate plans](https://github.com/guardian/price-migration-engine/pull/1026)
5. [The SalesforcePriceRiseCreation Step](https://github.com/guardian/price-migration-engine/pull/1020)
6. [The User Notification Step](https://github.com/guardian/price-migration-engine/pull/1023)
7. [The Amendment Step](https://github.com/guardian/price-migration-engine/pull/996)

## Prevent accidental firing of migration steps

There are situations where you have already implemented and excecuted the first steps of a migrations, but do not yet want to see the later steps being executed, or if, for some reasons, you want to permanently disable a step of a given migration, [as we did for SupporterPlus 2024](https://github.com/guardian/price-migration-engine/blob/2da72b6d02aa96c42781ea8a70b3431895f95af4/lambda/src/main/scala/pricemigrationengine/handlers/SalesforcePriceRiseCreationHandler.scala#L116). A very simple way to perform this is simply to add guards in the handlers. [See an example](https://github.com/guardian/price-migration-engine/pull/1141).

## Downloading the Cohort Tables

As part of preparing / running a migratition, it is often useful to run processes on the entire cohort table. It is not easy to query Dynamo tables directly but you can download the entirety of the records as JSON objects. Here is the command that Pascal has been using: (You only need the Janus credentials, also in this case given for the PriceMigration-PROD-GW2024 table)

"""
aws dynamodb scan --region eu-west-1 --table-name PriceMigration-PROD-GW2024 --select ALL_ATTRIBUTES --page-size 50000 --max-items 50000 --output json --profile membership > 02-data.json
"""

If the table that you are downloading has more than 50,000 items, and you want all of them, just update that number.

## When is a migration completed ?

A migration is completed when every item of the cohort table is in either of the following processing stages:

- `AmendmentWrittenToSalesforce`
- `ZuoraCancellation`
- `EstimationFailed`
- `NotificationSendFailed`
- `AmendmentFailed`
- `Cancelled`
