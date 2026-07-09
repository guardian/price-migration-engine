# Coding Directives

The price migration engine doesn't have coding conventions per se. ZIO does a very good job at keeping sanity between pure and impure code, and putting adhoc code into migration specific objects (the set up of the so called "modern" migrations) helps separate the general engine logic from specific requests. We also rely on the coding expertise of contributors to simply do the right thing (including breaking rules when needed).

With that said, we have the following conventions

### Coding Directive #1:

When using `MigrationType(cohortSpec)` to dispatch values or behaviour per migration, and unless exceptions (there are a couple in the code for when we handle exceptions or for exceptional circumstances), we will be explicit on what we want and declaring all the cases. If somebody is implementing a new migration and follows the steps Pascal presented during GW2024, then declaring a new case will happen during the [first step](https://github.com/guardian/price-migration-engine/pull/1012). The reason for this rule is that an inexperienced contributor could easily miss a place in the code where a new migration should specify behaviour. If the code compiles without prompting that decision, then the contributor might miss it. And even if the decision is to go with the "default", this needs to be explicitly specified. This convention was introduced in this pull request [pull:1022](https://github.com/guardian/price-migration-engine/pull/1022).

### Coding Directive #2:

This is more a design directive than a coding directive, but since the engine is just a backend process that performs operations in the morning on the set of subscriptions in specific processing states at specific dates, and in particular doesn't provide any user facing functionalities, it is much better to let the engine fail when it encounters conditions that are outside what it expects, than writing "clever" handling logic. In case of an error, an alarm is going to be issued (email, automatic chat message, etc) and engineers can then have a look at what the problem was.

### Coding Directive #3:

In this directive we confirm the fact that when defining non standard attributes for `EmailPayloadSubscriberAttributes` in the Notification handler, for instance

```
newspaper2025_phase4_brand_title = Some(productMigration2025N4NotificationData.brandTitle),
newspaper2025_phase4_formstack_url = Some(productMigration2025N4NotificationData.formstackUrl),
```

for ProductMigration2025N4, [currently visible here](https://github.com/guardian/price-migration-engine/blob/6ffafc7c998b55c30753282b98534fb3558fae8f/lambda/src/main/scala/pricemigrationengine/handlers/NotificationHandler.scala#L274), or

```
sp2026_contribution_amount = Some(supporterPlus2026ExtraData.contributionAmount),
sp2026_current_combined_amount = Some(supporterPlus2026ExtraData.currentCombinedAmount),
sp2026_new_combined_amount = Some(supporterPlus2026ExtraData.newCombinedAmount)
```

for SupporterPlus2026, then the function in the migration's own module, in the former case `ProductMigration2025N4Migration.getNotificationData` can return an optional value to be lifted up by `ZIO.fromOption` (optional because `getNotificationData` uses a `for` construct), but that value should never be `None`, otherwise this will prevent emails from other migrations from being sent. This is not something that can easily be detected in tests and just happens in production.

To prevent that value from being None, we need to supply a default value when the code is called from another migration type, for instance `SupporterPlus2026` calling `ProductMigration2025N4Migration.getNotificationData` as part of building the payload for `SupporterPlus2026`. We need a notion of
"empty" value that we can wrap in a `Some`. This explains expressions such as 

```
ProductMigration2025N4NotificationData("", "")
```

Where we have an empty string (the value to be used as attribute in the email payload).

Now the question is. Could not have `ProductMigration2025N4NotificationData` defined its own attributes as optional values ? allowing us to return

```
ProductMigration2025N4NotificationData(None, None)
```

The answer here is that this would complicate the code in the Notification handler, in the case of `Some` values.

One refactoring we could do one day that would simplify the situation and add flexibility is simply to define `EmailPayloadSubscriberAttributes` as a Map[String, String] rather than a Scala class used by all migrations. `EmailPayloadSubscriberAttributes` is indeed not used for anything else than generating JSON.
