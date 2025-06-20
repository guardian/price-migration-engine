
## Extending CohortItem

With the [introduction of migrationExtraAttributes](https://github.com/guardian/price-migration-engine/pull/1139), I do not expect `CohortItem`s to be extended any time soon, but if you do, do not forget to [map the dynamo record to the Scala type](https://github.com/guardian/price-migration-engine/pull/1158). It's not done automatically.

