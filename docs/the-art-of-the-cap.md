
# The art of the cap

Price capping was originally introduced in late 2022 to implement the newly introduced policy of not price rising customers by more than 20%. For instance, if the pre price rise value of the subscription was 100 and the estimated price was 130, then we would artificially set the price of the subscription to 120. 

1. This policy was originally introduced as a core function of the engine, which was a [big mistake](https://github.com/guardian/price-migration-engine/pull/781).
1. Was not applied to the 2023 digital migrations (hence, related code modifications to make it migration specific).
1. Been reintroduced in 2024 [as a library](https://github.com/guardian/price-migration-engine/pull/1006))
1. Updated in [2025](https://github.com/guardian/price-migration-engine/pull/1147)

## Logic of a price cap

After the Estimation step a cohort item has the current price of the subscription, which the engine learnt from Zuora, but also the estimated new price, the price Zuora indicated the subscription will have after the price rise amendment. This price is obviously the uncapped price.

Now one must resist the temptation of capping the estimated new price and storing that in the cohort item. It's a very bad idea that caused us a [lot of problems](https://github.com/guardian/price-migration-engine/pull/781) in the past. We now store the uncapped price, and only perform the capping, when needed, at specific places.

One must make sure that capping happens at these three places:

1. The Estimation step.
2. The Salesforce price rise creation step.
3. The Amendment step (from inside the migration module, when returning the Zuora Order)
