# Coding Conventions

The price migration engine doesn't have coding conventions per se. ZIO does a very good job at keeping sanity between pure and impure code, and putting adhoc code into migration specific objects (the so called "modern" migrations) helps separate the general engine logic from specific requests. We also rely on the coding expertise of contributors to do the right thing (including breaking rules when needed).

With that said, we have the following conventions

- When using `MigrationType(cohortSpec)` to dispatch values or behaviour per migration, and unless exceptions (there are a couple in the code for when we handle exceptions or for exceptional circumstances), we will be explicit on what we want and declaring all the cases. If somebody is implementing a new migration and follows the steps Pascal presented during GW2024, then declaring a new case will happen during the [first step](https://github.com/guardian/price-migration-engine/pull/1012). The reason for this rule is that an inexperienced contributor could easily miss a place in the code where some thought should have been given on what a new migration should specify. If the code compiles without prompting that decision the contributor might miss it. And even if the decision is to go with the "default", this needs to be explicitly specified. This convention was introduced in this pull request [pull:1022](https://github.com/guardian/price-migration-engine/pull/1022).

