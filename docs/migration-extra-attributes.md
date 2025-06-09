# Migration Extra Attributes

### Situation

Fundamentally migrations work around cohort items, which hold a referenece to a zuora subscription as well as meta data such as the timestamps of various operations or computed data such as the new price etc.

What cohort items never held before, was extra data provided by marketting about a subscription, aimed at affecting the way this subscription is to be processed.

This has changed with Guardian Weekly 2025, where we got the request to try and remove some existing discounts on old subscriptions that are part of the migration, and the list of which subscriptions are affected came as an Excel spreadsheet. (Note that it's not the first time that we have a request like this, and the way Pascal dealt with it was to hardcode that list of subscriptionIds into the migration module itself, which worked well, but it's not ideal to mix source code and data). Another possibility (better solution from an architectural point of view), would have been to store the data in a file in S3 and load it on demand. In the end the best solution is to put that extra data where it is more naturally placed: the cohort item.

### Why a string ?

We do not know in advance what the data is going to be, but more more importantly it can (and will!) change from one migration to another. Strings are versatile in the sense that one can encode data the way they want (for instance a comma separated string, or a json string), and deserialise it as required by the migration itself.

### How to set the attribute ? (part 1)

So assuming we have a migration that require the use of `migrationExtraAttributes` how do we set the correct value ?

Here, the current (1) recommendation is to perform as follows:

1. Create the migration Dynamo table, by uploading the list of subscription Ids as normal, meaning as described in [subscription-numbers-upload.md](./subscription-numbers-upload.md).
1. Use a dedicated script to set the attribute for each subscription (see "How to set the attribute ? (part 2)")

(1) This may change in the future.

### How to set the attribute ? (part 2)

One possible way to update the `migrationExtraAttributes` for a given cohort item is to use the `aws` command line tool.

```
aws dynamodb update-item \
    --profile mobile \
    --region eu-west-1 \
    --table-name 'TABLE_NAME' \
    --key '{"subscriptionId":{"S":"SUBSCRIPTION_NUMBER"}}' \
    --update-expression "SET migrationExtraAttributes = :attributeName" \
    --expression-attribute-values '{":attributeName":{"S":"ATTRIBUTE_VALUE"}}'
```

Pascal uses this tool from Ruby scripts to automate the updates.
