
## CohortItem

case class `CohortItem` carries the migration state as well as metadata for a subscription.

When expanding the `CohortItem` case class, do not forget to [map the dynamo record to the Scala type](https://github.com/guardian/price-migration-engine/pull/1158). It's not done automatically.

## Extending CohortItem (Part 1)

In 20205 Marketing asked for features that could not be naturally implemented against the then version of `CohortItem`. For this I introduced an extra field called `migrationExtraAttributes`. Its purpose was to carry a JSON string that would contains all the extra values migrations would require. An example from P3 is 

```
{ "brandTitle": "the Guardian", "earliestMigrationDate": "2025-10-06" }
```

Storing a JSON string in a text field has the advantage of allowing extensibility, but it's also a bit awkward to use, notably that this object was the result of several updates using dedicated scripts.

## Extending CohortItem (Part 2)

With ProductMigration2025N4 a new way to extend the CohortItem was adopted, which simply consists in adding extra fields to the case class. Fields dedicated to specific migrations; therefore we do not intend for them to be reused from one migration to another and they should be decommissioned together with the corresponding migration. Extra fields `ex_2025N4_label`, `ex_2025N4_group`, `ex_2025N4_canvas`, `ex_2025N4_rateplan_current` and `ex_2025N4_rateplan_target` were added for ProductMigration2025N4.
