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

# Run the import lambda

- Navigate to the [price-migration-engine-subscription-id-upload-lambda-PROD](https://eu-west-1.console.aws.amazon.com/lambda/home?region=eu-west-1#/functions/price-migration-engine-subscription-id-upload-lambda-PROD?tab=configuration)
lambda in the AWS console.
- Click the 'Test' button

