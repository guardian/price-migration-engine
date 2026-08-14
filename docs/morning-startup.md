
### How does the engine starts in the morning ?

The starting point is specified in the file: stateMachine/cfn/cfn.yaml. At 7am UTC, the lambda `price-migration-lambda-PROD` is fired up.

The lambda takes all the items in the Dynamo table `price-migration-engine-cohort-spec-PROD` and starts a price migration state machine passing in an object of the form

```
{
  "cohortSpec": {
    "cohortName": "Membership2025",
    "active": true
  }
}
```

Note that only cohort specs with `active` set to `true` will be fired up.
