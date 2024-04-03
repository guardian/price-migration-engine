# The migration implementation manual

In this chapter, we explain how to implement a migration from scratch.

## Introduction

In the chapter [Price migrations from first principles](https://github.com/guardian/price-migration-engine/blob/main/docs/price-migrations-from-first-principles.md) we have discovered why some commercial entities run price migrations. In this chapter I will be describing how we implement them at the Guardian. This description is almost entirely based on Pascal's experience in implementing and monitoring them, but if one day you are responsible for an implementation you may find easier to do things slightly differently.

Price migrations are not P&E projects. They are Marketing led projects. Marketing, in collaboration with Finance, will anounce that a collection of subscriptions in Zuora, often corresponding to all subscriptions of a particular product (for instance, Guardian Weekly subscriptions) will need to be migrated. The price migration engine orchestrates that operation from a systems point of view, but this happens within the larger context of operations ran by Marketing.

## Coding a migration. High level view

In principle migrations are fundamentally very easy to set up. You will be given a list of subscription ids to load into the engine (this happens by putting a file into a specific location in S3), then you will make a PR to membership-workflow to register the Braze campaign or canvas for the user notifications, and then you just need to add a new record to price-migration-engine-cohort-spec-PROD to ensure that the state machine runs every day with the migration parameters. The engine is fundamentally fully automatic and will operate by itself.

In practice, things are a bit more subtle. To be able to better serve the needs of Marketing, we moved from the idea of extremelly generic code that would apply to all migrations, to treating each migration as a separate event in the engine. The advantage is that it's now very easy to implement migration specific requests. The slight disadvantage is that a new migration requires a PR to be made against the engine code. Luckily we never run more than 3 or 4 migrations each year and if you know what you are doing you can encode them in a day.

In practice the mechanism we use to provide migration specific behavior and data is the `MigrationType` trait. In the current design of the engine the first step in starting a migration in to define a new instance of that trait.

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

## The membership-workflow update

A PR needs to be made to register in membership-workflow the name of the campaign (or canvas) the migration will use for user communication. An example (for Newspaper2024) can be found [here](https://github.com/guardian/membership-workflow/pull/505).
