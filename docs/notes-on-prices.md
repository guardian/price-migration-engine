## Notes on Prices

### General principles

Below, a sequence of events from the point of view of prices and some gotchas to be aware of

Chronological events:

1. The price migration target price rise is decided by Marketing.
2. The engine decides whether to apply the 20% cap (or is told not to).
3. The estimation step gives a value to `estimatedNewPrice`.
4. The amount we communicate to the users 30+ days before amendment is `estimatedNewPrice`.
5. The engine performs the amendment using `estimatedNewPrice` as reference, and then Zuora gives us `newPrice`.
6. The users are billed `newPrice`.

Gotchas:

- If there was a price cap, or a discount was added any time before (6.), the price decided at (1.) will not be the price at (6.)
- If the discount is added between (1.) and (3.), then the above is true, but at least there will be consistency between (3.), (5.) and (6.)
- More importantly, if a discount was added between (3.) and (6.) [1], then the amount we told the customer at (3.), will not be what they are billed at (6.) [2].

[1] There can be up to a year between (3.) and (6.)

[2] The billed amount will be lower.

Summary:

Discounts, price caps as well as subscription cancellations, are loss of revenue for the Guardian (here I am using the word "loss" in a neutral way, not as a problem to solve, just highlighting a difference between "ideal" and "actual"), but when discounts are added after the estimation step, they also cause a discrepancy between the user comms and the user invoice.

### The September 2025 price incident

Although price migrations are usually without calculations, print products migrations, due to the the structure of the rate plan and the numerous individual charges, cause the engine to calculate the amendment components from the `estimatedNewPrice` (as well as the previous charge distribution, to ensure a similar spectrum of charges). A floating number rounding error in the computation of the amendment components, caused the resulting `newPrice` as calculated by Zuora, to be a few pennies lower than the intended price (see correction here [price-migration-engine/pull/1233](https://github.com/guardian/price-migration-engine/pull/1233)). This was not detected by the existing post amendment price check because the check was not designed to alarm on lower `newPrice`.

It is interesting to notice that this floating number error didn't happen during previous multi charges print migrations (3+ years ago) because the prices were taken from the price catalogue, where they were "intended" by definition. Whereas newer migrations encode (and calculate) the Marketing price grid in the engine for flexibility and security.

It is also interesting to notice that Guardian Weekly, the print product we prise rised twice before 2025, didn't suffer from any of this, due to having only one charge.
