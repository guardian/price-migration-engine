# Importing Subscription Ids

The price migration engine needs to be initialised with the ids for subscriptions that are going to have price rises 
applied to them.

The process for uploading the subscription ids is as follows.

## Getting the subscription numbers

It is possible to get the subscription numbers from Salesforce, but good practice is simply to ask marketing to generate the list. It is possible to have more than one field in the csv output but only the first column is going to be read and interpreted as the subscription number.

# Upload the report file to S3

One can either use the aws console or upload the file from the terminal using the aws command line tool. 

To upload from the terminal:

- Get the 'membership' credentials from [Janus](https://janus.gutools.co.uk/) and add them to your local environment
- Copy the report csv file from above to the price migration engine s3 bucket
  ```bash
  aws --profile membership s3 \
    cp /path/to/file/subscription-numbers.csv s3://price-migration-engine-prod/<migration name>/subscription-numbers.csv
  ``` 

# Run the import lambdas

There are two lambdas responsible for creating the dynamo table and uploading the subscription numbers. They are 
  - price-migration-engine-table-create-lambda-PROD, and
  - price-migration-engine-subscription-id-upload-lambda-PROD

Do not forget to set the correct cohort specifications (see example below, but use the right values for your migration), as input data for the lambdas before you click on `Test`

```
{
    "cohortName":"migration-name",
    "brazeCampaignName":"any name",
    "importStartDate":"2024-02-01",
    "earliestPriceMigrationStartDate":"2024-05-20" 
}
```

