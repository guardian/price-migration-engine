# Notifications Windows

This file explains the notion of notification window which is central to the notification handler.

## General principles

Before increasing the price of a subscription, we need to notify the customer, and we need to do so
at least 30 days before the price increase (more exactly they need to have received the communication
at least 30 days before the price increase).

Historically the notifications were delivered to the customers as letters, but the membership
migration, in March 2023, introduced email notifications.

When we do letter notifications, the engine scans the migration cohorts and looks for items
which are in processing status `SalesforcePriceRiseCreationComplete` and with an
`amendmentEffectiveDate` (the date of the price increase for that subscription) which
is equal to or less than 49 days away (the value 49 was mostly used for old, pre 2023,
print migrations). In normal circumstances the engine will then send a notification to
Braze and move the items to processing stage `NotificationSendComplete`.
