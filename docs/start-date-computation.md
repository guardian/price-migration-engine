# The art of computing start dates

The engine code uses the variable name `startDate` to refer to the effective date of the price rise of a given subscription. That date is computed during the Estimation step. For instance, if subscription S-0000001 has a start date of `2024-08-27` (27th August 2024, which is in the future at the time these lines are written), then it means that on that day, the subscription is going to be billed using the intended updated price for this migration.

So we have just learnt something about start dates, they are billing dates. A monthly subscription that is billed on the 27th of each month, can only have a start date of the form `****-**-27`. A quarterly subscription of that pays on the 15th of January (and 15th of April, 17th of July, and 15th of August), can only have start dates from one of the following forms: `****-01-15`, `****-04-15`, `****-07-15` or `****-10-15`.

In this chapter, we learn how to compute start dates.

### Context

To be computed, a start date needs a context. here is the context we are going to work with:

First we need a cohort spec. Let's assume that the cohort spec is

```
{
    "cohortName":"GW2024",
    "brazeCampaignName":"SV_GW_PriceRise2024",
    "importStartDate":"2024-02-01",
    "earliestPriceMigrationStartDate":"2024-05-20" 
}
```

The only relevant bit of cohort spec we need for the computation of a subscription's start date is the `earliestPriceMigrationStartDate`. 

Let us assume thay the notification period for this migration is `[-49, -36]`. This is Pascal's notation for the fact that we start notifying at -49 days and alarm at -36.

Let us assume that our subscription is a monthly subscription paying on the 27th of each month, and let us assume that it was created on 8th July 2023.

Let us assume that for this migration, marketing decided that the spread period would be 3 months (see section below for the definition of the spread period).

And last, but not least, let's assume that today is 07th March 2024.

### A note on anniversaries.

Imagine that you work for HR and are given the task of giving a gift to the thousands of people working for a company. You can only give to somebody their gift on their birthday. It's going to take you an entire year to do so. If today is the 7th of March 2024, then you will give gifts to a bunch of people today (all the people with a 7th of March birthday) and you will finish on the 6th of March 2025.

If you are then told to not use the yearly birthday, but simply the number of their birthdate, so for instance if somebody is born on Dec 23rd, you can give them their gift on 23rd of March. Then, it will take you one month to give all the gifts away. You will start today on 7th of March and will finish on 6th of April.

But then you realise that if you use the monthly anniversary of the person, then there are many more people to give a gift to per day (exactly 12 times more people actually), and you can't run that fast. So you decide of a spread period. You decide that you will do it over three months. So you attribute a random number from the set { 0, 1, 2 } to each person on your distribution spreadsheet, and if the number is 0 you give them the gift the next available day number, otherwise you postpone by that random choice number of months.

### Spread periods

When Marketing says that they want to spead the monthlies over 3 months, they mean that the process of upgrading the monthlies to a new price (which should otherwise only need 1 month) should be planned to take 3 months. You know that there will be a random choice made as part of the decision of the subscription start date. 

We could, but we never use a spread period for other billing frequencies (quarterly, annual, etc)

### Why do we use spread periods

If we have a lot of monthlies to migrate, that's a lot of user notifications per day and therefore a certain number of people calling the Guardian call centers per day. By spreading the monthlies over 3 months instead of 1 month, we help the call center operators with not being flooded with daily calls.

### Computing the start date.

With the above context and the explanation about spread periods, let's compute the start date of our subscription.

Step 1: We know that the chosen date needs to be after the cohort spec's `earliestPriceMigrationStartDate`, meaning after `2024-05-20`. The date `2024-05-20` is our first lowerbound.

Step 2: We know that the date needs to be after today plus the end of a notification period (otherwise the engine will alarm immediately). Remember we are using `2024-03-07` as "today". The notification period is `[-49, -36]`, so there should be at least 37 days between today and the chosen date. This means thay we have a lowerbound at `today + 37 days`, meaning `2024-03-07 + 37 days`, meaning `2024-04-13` (13th of April).

Step 3: Our new lowerbound is the max of the two lowerbounds we have computed so far. `max(2024-05-20, 2024-04-13) = 2024-05-20`. So the start date should be after `2024-05-20`.

Step 4: We now need to apply our 1 year anniversary policy. The subscription was created on 8th July 2023, meaning `2023-07-08`. We cannot price rise it before `2024-07-08`. Therefore our new lowerbound is `max(2024-05-20, 2024-07-08) = 2024-07-08`.

Step 5: Our spread period is 3 months. Let's assume that the random choice returned 1, We now need to add a month to our previously computed date, which is now `2024-08-08`. 

Step 5: We are now ready for the start date. The start date is the next available billing date after `2024-08-08`. Since we have a monthly sub paying on the 27th, the start date is `2024-08-27` 🗓️ 🎉








