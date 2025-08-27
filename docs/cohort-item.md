
## CohortItem

Cohort items correspond to records of price or product migrations that represent the current state of the migration of a particular subscription. 

In [The journey of a cohort item](./the-journey-of-a-cohort-item.md), you can learn about the different stages (processing stage) that they go through.

Here we are mostly focusing on the extended attributes.

### Cohort Items Extended Attributes

The extended attributes `extendedAttribute1` to `extendedAttribute6` were introduced in August 2025, as the updated version of the `migrationExtraAttributes` key that was introduced earlier in the year to help implementing extra functionalities for the print migrations that started over the summer (we needed to encode labels and booleans).

Each attribute doesn't have a predetermined role in a generic migration and can be used for each specific migration to carry extra data needed by that particular migration. For instance for a given migration `extendedAttribute1` could be a date (of relevance for that specific migration), while for another migration it could be a numerical value (of relevance and significance only for that migration). Because we do not know in advance what the prefered datatype might be, they are all defined as `String`, and it is the responsibility of a specific migration to clarify what they are used for (in the notes for that migration), and perform the relevant casting (in the code of that migration).

Why 6 of them? 

That's was the current count at the time we introduced them, but it's possible to introduce new ones (`extendedAttribute7`, etc) in the future would the need arise.

### Extending Cohort Items

When you extend CohortItem, do not forget to [map the dynamo record to the Scala type](https://github.com/guardian/price-migration-engine/pull/1227), in `CohortTableLive.scala`. It's not done automatically.

