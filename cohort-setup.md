# How to set up a new cohort

Previously, cohorts have been separated by product type, i.e.: Newspaper Vouchers and Newspaper Delivery.  The cohort names reflected this, eg. 'Vouchers2022'.  This was because the engine was being developed to support new product types while it was in use.  But cohorts don't need to be product-specific and the whole lot could be put in a single cohort.  How the cohorts are broken down depends on which subscriptions are ready for migration, either for business or technical reasons.

The following are the steps required to set up a new cohort of subscriptions due to have their price increased.

# IMPORTANT: BEFORE RUNNING A COHORT

If using a new Braze Campaign, the campaign name, and it's ID must be added to the [configuration file](https://github.com/guardian/membership-workflow/blob/main/conf/PROD.public.conf#L119-L123) in the `membership-workflow` repo, under `directMailCampaigns`.

 This will map the campaign name from the DynamoDB table to it's correct ID in `membership-workflow`. We can do [future work](https://trello.com/c/5VWs9qNV/480-do-not-require-the-braze-campaign-id-to-be-added-to-membership-workflow-rather-provide-it-through-dynamodb) to prevent having to do this, and rather provide the campaign ID directly in the DynamoDB table.

## 1. Cohort specification

From the DynamoDB Dashboard, click on `Explore items` in the navigation menu. Create an item in the `price-migration-engine-cohort-spec-PROD` DynamoDB table.  
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

Add a folder to the `price-migration-engine-prod` S3 bucket with the same name as the cohort in the cohort specification
table.  
Add two CSV files to the folder:
* **salesforce-subscription-id-report.csv**: A file holding line-separated subscription numbers to be **included** in the 
price rise, which will typically be the result of a Salesforce report. More info in [ImportSubscriptionId.MD](./ImportSubscriptionId.MD) on this. 
* **excluded-subscription-ids.csv**: A file holding line-separated subscription numbers to be **excluded** from the price 
rise.

## 3. Run the state machine or lambda

Navigate to the `price-migration-lambda-PROD` and enter the cohort spec to kick off the price-rise process. To see examples of previous executions and cohort Spec's, navigate to `price-migration-engine-cohort-steps-PROD` from the Step Functions dashboard.
Example cohort spec: `{"cohortSpec":{"cohortName":"HomeDelivery2022","brazeCampaignName":"SV_PrintPriceRise_2022","importStartDate":"2022-04-28","earliestPriceMigrationStartDate":"2022-06-29"}}`

A new cohort table will be created. The name of the table contains the stage and the Cohort name specified in the CohortSpec table. 

## Running a test cohort

See [salesforce-subscription-id-report.csv](./lambda/src/test/resources/salesforce-subscription-id-report.csv) for an example ID report. Upload a blank csv file for the excluded-subscription-ids.csv.

**Note on membership-workflow**

[membership-workflow](https://github.com/guardian/membership-workflow) will fail due to improper test data:

- Any emails ending with @gu.com are excluded from the queue.
- Emails have to be linked to a valid identity user.

Use the following salesforce query to browse subscriptions for testing:

`SELECT
    Id,
    Buyer__r.Email,
    Buyer__r.IdentityID__c,
    Zuora_Subscription_Name__c,
    Product_Name__c,
    Rate_Plan_Name__c,
    Promotion_Discount__c,
    Acquisition_Date__c,
    Zuora_Billing_account__r.Zuora__MRR__c
    FROM SF_Subscription__c
WHERE
    Product_Name__c IN ('Newspaper Delivery') AND Status__c IN ('Active')
LIMIT 500
`

Any subscription with numerical Identity ID's should work (e.g.: 200004527), the non-numerical ones (e.g.: Test_Scenario6-1) fail in membership-workflow.
