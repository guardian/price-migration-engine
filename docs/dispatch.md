
## Dispatch, dealing with unusually large migrations

30 July 2026

SupporterPlus2026 is a record breaking price migration with a cohort table containing more than 350,000 items, which is more than 3 times the previous largest migration. One interesting side effect of this large number is that at the moment (and this will continue for an entire month) the daily load of Braze user notification and Zuora amendments takes more than 24 hours. This is due to the fact that we process cohort items one by one.

Here is a possible solution of how to deal with those [https://github.com/guardian/price-migration-engine/pull/1500](https://github.com/guardian/price-migration-engine/pull/1500).

Note that this is a better solution than spliting the cohort table (what was referred to as Solution 2 in the PR description), because we regularly perform adhoc tasks for Marketing that would be more difficult to perform is the items of a single migration were split in several tables.
