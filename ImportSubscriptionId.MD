# Importing Subscription Ids

The price migration engine needs to be initialised with the ids for subscriptions that are going to have price rises 
applied to them.

The criteria for selecting the subscriptions to be migrated are set out here:

https://docs.google.com/spreadsheets/d/1J4g_fn3DfSwfSljRg6HrBlLBV84BJWElCFGkunU4P0c/edit#gid=0

The process for uploading the subscription ids is as follows.

## Download subscription ids from salesforce

See [subscription-numbers.csv](./lambda/src/test/resources/cohort-files/subscription-numbers.csv) for required format of the CSV file.
**Note:** Although the salesforce report may contain many fields for each subscription, only the ID is required, and it is required as the first column of the CSV file, see the writeSubscriptionIdsToCohortTable function in SubscriptionIdUploadHandler.scala file for how it reads the CSV and creates a cohort item out of it.

This report contains the subscriptions that will be included in the price migration process.

- Go to the salesforce report containing the subscrptions:
  [https://gnmtouchpoint.my.salesforce.com/00O0J000007sJHs/e?retURL=%2F00O%3Ffcf%3D00B0J000008lXV2%26rolodexIndex%3D-1%26page%3D1](https://gnmtouchpoint.my.salesforce.com/00O0J000007sJHs/e?retURL=%2F00O%3Ffcf%3D00B0J000008lXV2%26rolodexIndex%3D-1%26page%3D1)
- Click the 'run' button
- Click the triangle icon in the top right of the screen and click 'Export'  
- Click 'Details only'
- Select CSV format
- Select UTF-8 encoding
- Click the export button
- Make a note of the report file name

# Download the subscription exclusions csv


# Upload the report file to S3

- Get the 'membership' credentials from [Janus](https://janus.gutools.co.uk/) and add them to your local environment
- Copy the report csv file from above to the price migration engine s3 bucket
  ```bash
   aws --profile membership s3 cp ~/Downloads/<subscription numbers>.csv s3://price-migration-engine-prod/subscription-numbers.csv
  ``` 

# Run the import lambda

- Navigate to the [price-migration-engine-subscription-id-upload-lambda-PROD](https://eu-west-1.console.aws.amazon.com/lambda/home?region=eu-west-1#/functions/price-migration-engine-subscription-id-upload-lambda-PROD?tab=configuration)
lambda in the AWS console. 
- Click the 'Test' button

  