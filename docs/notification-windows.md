# Notifications Windows

This file explains the notion of notification window which is central to the notification handler.

## General principles

Before increasing the price of a subscription, we need to notify the customer, and we need to do so at least 30 days before the price increase (more exactly they need to have received the communication at least 30 days before the price increase).

Historically the notifications were delivered to the customers as letters, but the membership migration, in March 2023, introduced email notifications.

When we do letter notifications, the engine scans the migration cohorts and looks for items which are in processing status `SalesforcePriceRiseCreationComplete` and with a `startDate` (the date of the price increase for that subscription) which is equal to or less than 49 days away (the value 49 was mostly used for old, pre 2023, print migrations). In normal circumstances the engine will then send a notification to Braze and move the items to processing stage `NotificationSendComplete`.

It may happen (has happend during the early days of the engine) that for some reason the engine is down while problems are investigated. If it is down, for instance, for 20 days, then when it comes back up, some items will be 29 days away from their startDate. To prevent them being picked up by the engine for notification the engine performs another check, that the items are at least 35 days away. This padding of 5 days is to ensure that letters do get to their destination before being 30 days away. Anything less than 35 and the engine will fail.

The natural window period for letters is now defined with the functions `maxLeadTime` and `minLeadTime`.

The max is included but not the min. In other words, if the item is exactly max days away in the future, it is going to be picked up, but if it is exactly min days away then an error will occur.

## The membership migration (2023)

For the membership migration, the values returned by `maxLeadTime` and `minLeadTime` are, respectively, 33 and 31. This is a significantly tighter (2 days) notification period than for the letters (emails are sents at -33 days or minus -32 days). It also is dramatically closer to the legal 30 days. Although, in this case, we do not need to worry about delivery time.
