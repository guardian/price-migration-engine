
## CohortSpec

CohortSpec is the object that lambdas of the state machine take as input. They mostly specify the particular cohort that the lambda is going to run against (`cohortName`).

An additional mandatory attribute is the earliest amendment effective date, which is used as the starting point of the notifications and amendment dates. See [amendment-effective-date-computation.md](./amendment-effective-date-computation.md) for details.

We also have two optional attributes: `subscriptionNumber` and `forceNotifications`. 

Attribute `subscriptionNumber` can be used to limit the estimation handler, the notification handler and the amendment handler to a single subscription, and this only works if the subscription was naturally eligible for that step. 

Attribute `forceNotifications` can be used to let a subscription go through the Notification step after the notification window. This can be used is we have exited the notification window but are still before the 30 days deadline.

