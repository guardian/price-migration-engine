# Operational Directives

The engine strive to be a fully automatic system, but occasionally a manual operation is required. Of course there are manual operations when setting up a new migration, but some conditions need a manual intervention from the stewards.

- The engine alarms. That typically happens in the morning and should be looked at. Nobody is immediately affected when that happens but the alarm could be related to a time critical operation.

- If the engine stays un-operational for a long time, some cohort items in processing stage `SalesforcePriceRiseCreationComplete` will have exited their notification window, meaning will be to close to the price rise billing date to be notified (they will end up in `SNARMissingNotificationWindow` status in the Notification handler). The solution here is simply to move them to `ReadyForEstimation`.