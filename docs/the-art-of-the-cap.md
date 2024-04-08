
# The art of the cap

In this chapter we explain the reasons for the current logic of the price capping function of the engine, which is currently found in the PriceCap object.

Price capping was originally introduced in late 2022 to implement the newly introduced policy of not price rising customers by more than 20%. For instance, if the pre price rise value of the subscription was 100 and the estimated price was 130, then we would artificially set the price of the subscription to 120. This policy was originally introduced as a core function of the engine (which was a mistake -- hence its [recent re-introduction as a library](https://github.com/guardian/price-migration-engine/pull/1006)), and also was not applied to the 2023 digital migrations (hence, recent code modifications to make it migration specific).

## The `ZuoraSubscriptionUpdate` data type

The mechanism by which we can set subscription prices on a subscription basis relies on what actually happens during a price rise. During a price rise we either migrate the subscription to a new rate plan or force the subscription to adopt the new pricing of the subscription it already had. In either case the target rate plan is the one defined in the product catalogue.

The important thing to understand is that when a subscription adopts a rate plan (or readaopts it), this is not represented in Zuora as a link from the subscription to the rate plan in the product catalogue. Instead, Zuora makes a copy of the product catalogue rate plan and attach it to the subscription. This is the moment one can override the product catalogue price and set whatever we want, including a price that has been capped.

When we perform the price rise update, we submit the following data type to Zuora

```
case class ZuoraSubscriptionUpdate(
    add: Seq[AddZuoraRatePlan],
    remove: Seq[RemoveZuoraRatePlan],
    currentTerm: Option[Int],
    currentTermPeriodType: Option[String]
)
```

The `RemoveZuoraRatePlan` essentially carries the id of the particular rate plan the subscription was carrying. The `AddZuoraRatePlan` indicates the id of the rate plan in the product catalogue (that rate plan is going to be copied as we said earlier and will then be given its own individual id). 

The exact shape of a `AddZuoraRatePlan` is

```
case class AddZuoraRatePlan(
    productRatePlanId: String,
    contractEffectiveDate: LocalDate,
    chargeOverrides: Seq[ChargeOverride] = Nil
)
```

and the important bit for price capping is `ChargeOverride`. If `chargeOverrides` is null then the rate plan given to the subscription will carry the price from the product catalogue. But if provided `ChargeOverride` carries the prices we want to apply. 

Note that the rest of this chapter conly applies to migration where we apply a price cap.

## Logic of a price cap

After the Estimation step a cohort item has the current price of the subscription, which the engine learnt from Zuora, but also the estimated new price, the price Zuora indicated the subscription will have after the price rise amendment. This price is obviously the uncapped price.

Now one must resist the temptation of capping the estimated new price and storing that in the cohort item. It's a very bad idea that caused us a lot of problems in the past. In the updated logic we store the uncapped price, and only perform the capping, when needed, at specific places.

To fully understand why the capping is the way it is, we need to work with a rate plan that has more than one charge. (In the case of one charge the trivial approach works).

So let's consider a print product with Saturday and Sunday deliveries and the rate plan (called "Week End") has two charged, like this

```
Week End 
    - Saturday: 12
    - Sunday  : 15
```

Zuora is going to say that the current price of the subscription is 27, but let's also assume that it says that the price after price rise would be 40, and that we have a 25% price increase cap. Since  27 + 25% (of 27) is 33.75, the price is going to be capped. As indicated above we do not store 33.75 in the cohort item as estimated new price we store 40. So at this point the cohort item is 

```
Item

    - current price      : 27
    - estimated new price: 40
```

and, very important, note that the cohort item doesn't know that the subscription has two charges

### At the Notification step

The first time we are going to call PriceCap is during the notification step. We are going to call the function 

```
priceCapForNotification(oldPrice: BigDecimal, newPrice: BigDecimal, cap: BigDecimal): BigDecimal
```

like this 

```
priceCapForNotification(27, 40, 1.25)
```

The returned value, 33.75, is the price we are going to tell the customer that they are going to pay. Note that the engine doesn't know, and doesn't care, that the old price and the new price come from two charges.

### At the SalesforcePriceRiseCreationHandler

The second time we are going to call `PriceCap` is inside the SalesforcePriceRiseCreationHandler, and we call `priceCapForNotification` again. We are informing Salesforce of the new price the users was told they are going to pay.

### At the Amendment step

At the Amendment step, we call `PriceCap` again but a different function. We call 

```
priceCapForAmendment(
    oldPrice: BigDecimal,
    newPrice: BigDecimal,
    cap: BigDecimal,
    zuoraUpdate: ZuoraSubscriptionUpdate
)
```

The logic of this function is that we provide it with the information we have, the old and new (uncapped) prices, the cap (which is a paramter of the migration) and the original `ZuoraSubscriptionUpdate`. The return value will be a modified `ZuoraSubscriptionUpdate`, where all the charges inside the `AddZuoraRatePlan` will be updated so that their sum end up being the capped price 🎉

And this is why we needed to work with the uncapped price. The updated charged are computed by applying a correction factor to the product catalogue charges, but that correction factor is a function of the old price and the uncapped new price. If we only had the old price and the capped price we would not be able to compute the correction factor, and would not be able to construct a correct `ZuoraSubscriptionUpdate`.

## Conclusion

The above should provide a clear explanation of why PriceCap has those two functions, `priceCapForNotification` and `priceCapForAmendment`, and why their signatures are the way they are. Note that we did not use `priceCapForAmendment` in GW2024, because we used `priceCapForNotification` directly in the construction of `ZuoraSubscriptionUpdate` (which was possible because we only have one charge). In any case, and as long as the existing library presents itself thise way, if a migration needs capping, one must make sure that capping happens at these three places:

1. The Estimation step.
2. The Salesforce price rise creation step.
3. The Amendment step.
