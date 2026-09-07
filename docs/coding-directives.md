# Coding Directives

The price migration engine doesn't have coding conventions per se. ZIO does a very good job at keeping sanity between pure and impure code, and putting adhoc code into migration specific objects (the set up of the so called "modern" migrations) helps separate the general engine logic from specific marketing migrations. We also rely on the coding expertise of contributors to simply do the right thing (including breaking rules when needed).

With that said, we have the following conventions

### Coding Directive #1:

When using `MigrationType(cohortSpec)` to dispatch values or behaviour per migration, and unless exceptions (there are a couple in the code for when we handle exceptions or for exceptional circumstances), we will be explicit on what we want and declare all the cases, in particular we do not use wildcards or catch-all cases. ( If somebody is implementing a new migration and follows the steps Pascal presented in the [migration implementation manual](https://github.com/guardian/price-migration-engine/blob/325c511064f8a3ed2320355d40385386fbed25da/docs/migration-implementation-manual.md), then declaring a new case will happen during the [first step](https://github.com/guardian/price-migration-engine/pull/1012) ). The reason for this rule is that an inexperienced contributor could easily miss a place in the code where a new migration should specify behaviour and if the code compiles without prompting that decision, then the contributor might miss it. And even if the decision is to go with the "default", this needs to be explicitly specified. This convention was introduced in this pull request [pull:1022](https://github.com/guardian/price-migration-engine/pull/1022).

### Coding Directive #2:

This is more a design directive than a coding directive, but since the engine is just a backend process that performs operations in the morning on the set of subscriptions in specific processing states at specific dates, and in particular doesn't provide any user-facing functionalities, then it is much better to let the engine fail when it encounters conditions that are outside what it expects than writing "clever" handling logic. In case of an error, an alarm is going to be issued (email, automatic chat message, etc) and engineers can then have a look at what the problem was.
