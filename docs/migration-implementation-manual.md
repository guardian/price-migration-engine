# The migration implementation manual

In this chapter, we explain how to implement a migration from scratch.

## Introduction

In the chapter "Price migrations from first principles" we have discovered why some commercial entities run price migrations. In this chapter I will be describing how we implement them at the Guardian. This description is almost entirely based on Pascal's experience in implementing and monitoring them and if one day you are responsible for an implementation you may find easier to do things slightly differently.

Price migrations are not P&E projects. They are marketing led projects. Marketing, in collaboration with Finance, will anounce that a collection of subscriptions in Zuora, often corresponding to all subscriptions of a particular product (for instance, Guardian Weekly subscriptions) will need to be migrated. The price migration engine orchestrates that operation from a systems point of view, but this happens within the larger context of operations ran by Marketing.

## High level view of an implementation

In principle migrations are fundamentally very easy to set up. You will be given a list of subscription ids to load into the engine (this happens by putting a file into a specific location in S3), then you will make a PR to membership-workflow to register the Braze campaign or canvas for the user notifications, and then you just need to add a new record to price-migration-engine-cohort-spec-PROD to ensure that the state machine runs every day with the migration parameters. The engine is fundamentally fully automatic and will operate by itself.

In practice, things are a bit more subtle. To be able to better serve the needs of Marketing, we moved from the idea of extremelly generic code that would apply to all migrations, to treating each migration as a separate event in the engine. The advantage is that it's now very easy to implement migration specific requests. The slight disadvantage is that a new migration requires a PR to be made against the engine code. Luckily we never run more than 3 or 4 migrations each year and if you know what you are doing you can encode them in a day.





