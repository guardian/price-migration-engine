# How to set up a new cohort

These are the steps required to set up a new cohort of subscriptions due to have their price increased.

## 1. Cohort specification

Add an item to the `price-migration-engine-cohort-spec-PROD` DynamoDB table.  
Fields:
* **cohortName**: A unique name to identify the cohort.  
Must consist of alphanumeric, whitespace, '-' and '_' characters.
* **brazeCampaignName**: The name that membership-workflow uses to refer to the Braze campaign for notifying subscribers
that the price rise is about to happen.  
Must consist of alphanumeric, whitespace, '-' and '_' characters.
* **importStartDate**: Date on which to begin importing participating subscription numbers into the engine.  
Format is `yyyy-mm-dd`.
* **earliestPriceMigrationStartDate**: Earliest date on which a subscription can have its price increased.  Increases
will always begin on the first day of a billing period on or after this date.  
Format is `yyyy-mm-dd`.

## 2. Source files

Add a folder to the `price-migration-engine-prod` bucket with the same name as the cohort in the cohort specification
table.  
Add two CSV files to the folder:
* **salesforce-subscription-id-report.csv**: A file holding line-separated subscription numbers to be **included** in the 
price rise, which will typically be the result of a Salesforce report.
* **excluded-subscription-ids.csv**: A file holding line-separated subscription numbers to be **excluded** from the price 
rise.
