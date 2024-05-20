# Troubleshooting

This document assumes there has been a failure either in the triggering lambda `price-migration-lambda-PROD` or
the cohort state machine `price-migration-engine-cohort-steps-PROD`.

To find detail on the failure:
1. Go to the cohort state machine `price-migration-engine-cohort-steps-PROD`.
1. Find the failing execution.
1. Open the cloudwatch link from the failing step.

## General tips

### Timeouts

One cause of failure is a read or socket timeout in an API. [Restarting the failing state machine](#manually-starting-another-cohort-state-machine-execution) is the best way to deal with timeouts. If this doesn't work, check for a general failure in the API and try again later.

If the cohort state machine fails, here are some suggestions depending on which step failed:

## Step failures

### NotifyingSubscribers

The most likely cause of a failure in this step is a missing country in the contact billing address. If this happens, the best solution is to fill in the country in the Salesforce record as it's usually quite obvious what it is.  Then [restart the state machine](#manually-starting-another-cohort-state-machine-execution).

It may be that other required fields are missing in the contact billing address. The mailing address is used as a fallback if the billing address is missing entirely. If there are other missing fields and it's not obvious how to fix them up in Salesforce, the best solution is to [put the sub into the EstimationFailed holding state](#moving-a-subscription-into-a-holding-state) and [restart the state machine](#manually-starting-another-cohort-state-machine-execution).

### Amending

A failure here is possible when a subscription is in an unexpected state at the time when the amendment is applied. If the failure is caused by a bad subscription, the best action is to [put the sub into the AmendmentFailed holding state](#moving-a-subscription-into-a-holding-state) and then [restart the state machine](#manually-starting-another-cohort-state-machine-execution).

## Interventions

Any manual intervention that's required should be logged somewhere to see if it could be programmatically avoided in future.

Here are the main possible interventions:

## Manually starting another cohort state machine execution

1. In the AWS console, find the state machine called `price-migration-engine-cohort-steps-PROD`.
1. Open the execution that failed.
1. Click `New execution` to kick-off an execution with the same input as the one that failed.

## Moving a subscription into a holding state

There are two holding states: `EstimationFailed` and `AmendmentFailed`. If a sub is in either of these states, it will be ignored for future processing. To move a sub into a holding state:

1. Go to the DynamoDB table `PriceMigrationEnginePROD`
1. Query using the partitioning key index for the subscription number.
1. Change the `processingStage` attribute to either of the holding states, depending on where the failure occurred.

## Manually restarting the triggering lambda

This will start up a cohort state machine for each active cohort.

:warning: **Check that there are no cohort state machines running before doing this! Running two state machines over the same DynamoDB table could lead to dirty reads.**

1. Check state machine `price-migration-engine-cohort-steps-PROD`. If there are no running executions:
1. In the AWS console, find the lambda called `price-migration-lambda-PROD` and `Test` it with a `null` input. The lambda should complete successfully in a few seconds.
1. If you then look at the state machine called `price-migration-engine-cohort-steps-PROD`, you should see a new execution
for each active cohort.
