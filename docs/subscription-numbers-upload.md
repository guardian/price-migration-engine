# Importing Subscription Ids

The price migration engine needs to be initialised with the ids for subscriptions that are going to be price risen. This list of ids is important because we do not generally price rise an entire product, but only specific subscriptions determined using attributes that Marketing are interested in.

The process for uploading the subscription ids is as follows.

## Getting the subscription numbers

It is possible to get the subscription numbers from Salesforce, but good practice is simply to ask Marketing to generate the list. They will probably provide a spreadsheet with more than one field in the csv output. As far as the engine is concerned, only the first column is going to be read and interpreted as the set of subscription numbers. (It is good practice to generate a separate file with only the subscription numbers and use that new file instead.)

## Upload the report file to S3

One can either use the aws console or upload the file from the terminal using the aws command line tool. 

### Direct upload into S3

Simply upload the file at `s3://price-migration-engine-prod/<migration name>/subscription-numbers.csv`

### Upload from the terminal

- Get the 'membership' credentials from [Janus](https://janus.gutools.co.uk/) and add them to your local environment
- Copy the csv file from above to the price migration engine s3 bucket, using the following command

```
aws --profile membership s3 \
  cp /path/to/file/subscription-numbers.csv s3://price-migration-engine-prod/<migration name>/subscription-numbers.csv
```

## Run the import lambdas

There are two lambdas responsible for creating the dynamo table and uploading the subscription numbers. They are 
  - price-migration-engine-table-create-lambda-PROD, and
  - price-migration-engine-subscription-id-upload-lambda-PROD

Do not forget to set the correct cohort specifications (see example below, but use the right values for your migration), as input data for the lambdas before you click on `Test`

```
{
    "cohortName":"migration-name",
    "brazeCampaignName":"any-name",
    "importStartDate":"2024-02-01",
    "earliestPriceMigrationStartDate":"2024-05-20" 
}
```

