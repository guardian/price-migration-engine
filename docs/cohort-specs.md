
## CohortSpec

CohortSpec is the object that lambdas of the state machine take as input. They mostly specify
the particular cohort that the lambda is going to run against (`cohortName`).

An additional mandatory attribute is the earliest amendment effective date, which is used as
the starting point of the notifications and amendment dates.
See [amendment-effective-date-computation.md](./amendment-effective-date-computation.md) for details.

We also have one optional attributes: `subscriptionNumber`.

Attribute `subscriptionNumber` can be used to limit the estimation handler, the notification
handler and the amendment handler to a single subscription, and this only works if the
subscription was naturally eligible for that step.
