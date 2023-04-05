# Notifications Periods

This file explains the notion of notification window which is central to the notification handler.

## General principles

Before increasing the price of a subscription, we need to notify the customer, and we need to do so at least 30 days before the price increase (more exactly they need to have received the communication at least 30 days before the price increase).

Historically the notifications were delivered to the customers as letters, but the membership migration, in March 2023, introduced email notifications.

When we do letter notifications, the engine scans the migration cohorts and looks for items which are in processing status `SalesforcePriceRiseCreationComplete` and with a `startDate` (the date of the price increase for that subscription) which is equal to or less than 49 days away. In normal circumstances the engine will then send a notification to Braze and move the items to processing stage `NotificationSendComplete`.

It may happen (has happend during the early days of the engine) that for some reason the engine is down while problems are investigated. If it is down, for instance, for 20 days, then when it comes back up, some items will be 29 days away from their startDate. To prevent them being picked up by the engine for notification the engine performs another check, that the items are at least 35 days away. This padding of 5 days is to ensure that letters do get to their destination before being 30 days away. Anything less than 35 and the engine will fail.

The natural window period for letters is defined with

```
val guLettersNotificationLeadTime = 49
val engineLettersMinNotificationLeadTime = 35
// the notification period is -49 (included) to -35 (excluded) days
```

The functions that actually define the max value and min value are `maxLeadTime` and `minLeadTime` [1]. The max is included but not the min, in other words, if the item is exactly max days away in the future, it is going to be picked up, but if it is exactly min days away then an error will occur.

[1] They were introduced to gracefully handle the letter and email notifications.

## The membership migration

For the membership migration we use a different notification period, defined by

```
val membershipPriceRiseNotificationLeadTime = 33
val membershipMinNotificationLeadTime = 31
// the notification period is -33 (included) to -31 (excluded) days
```

This is a tighter (2 days) notification period than for the letters (Eemails are sents at -33 days or minus -32 days). It also is dramatically closer to the legal 30 days. Although, in this case, we do not need to worry about delivery time.

## Moving the runtime to 11am during membership.

During the the first stage of the membership migration the engine was set to run at 11am UK summer time. As a consequence and to compensate for any resulting delay in the letters being sent to Latcham for delivery, we have (temporarily) increased the letter max time from 49 days to 50 days.

[This PR](https://github.com/guardian/price-migration-engine/pull/820) shows that update and explain when we will be able to move back to 7am UTC.

