# Price Migration Engine Data-Lake Export

This describes the solution and the reasoning behind it for the export of CohortItems, the information about the processing of price rises by the price migration engine.

## Possible Approaches
### Solution1 - Zuora/Salesforce approach
This approach starts with us exporting a ‘raw’ file to a s3 bucket, containing data from any active cohorts. The file would contain the complete set of data for all active cohorts. This data is merged, by a spark etl job, with existing data in a ‘clean’ bucket and written in ORC to the clean bucket.
- Pros
  - This is the same as all our other jobs
  - Possible future requirements for transformation/filtering can be done in the etl pipeline
  - Alerting in the normal way, ie when no data shows up in the raw directory, would be easy and work well. 
-Cons
  - The etl job is triggered by a single file appearing in s3. This may not be the case as we may have multiple cohorts running at the same time. We would need to either:
    - Write data for all active cohorts in a single file, which adds complexity to the export job and to the scheduling of the export job.
    - Trigger the job in airflow using a Cron schedule ie just run the etl job at a particular time each day regardless of whether a file has been uploaded
  - Depending on how the job is triggered, this might be wasteful in terms of cost of running EMR resources. It would be difficult to stop the triggering of the etl job when no cohorts were ‘Active’
    - We could always write a file and leave it empty if there are no active cohorts. This could cause the unnecessary startup of a EMR cluster.
    - We could not write a file if there is no data. This would make alerting difficult as we would not know if the process was broken or if there was no data.

### Solution 2 - Writing individual cohort files
This approach would run the export job as part of the state machine processing each cohort. The export would write a file with a name specific to that cohort. The file would be written to the ‘clean’ directory in s3 overwriting any previous file for that particular cohort.

- Pros
  - Simple to implement in the export code. 
  - No ETL code needs to be written.
  - Does not require any etl code to merge data as this is taken care of by partitioning, ie using separate files for each cohort.
- Cons
  - We would need to rely on alerting of failures in the price migration engine for monitoring as there would be no real active processing in the etl pipeline to provide altering. It would also, as above, be difficult to distinguish in the datalake code, between there being no data and something being broken.
  - Its the preference of the data tech team that we write the files in ORC/parquet which is slightly more complicated than csv.
  - Possible future requirements around filtering/transformation of the data in etl would be more difficult as the etl code would need to be implemented, including solving the difficulties around having multiple files with different names arriving an a ‘raw’ bucket


## Proposal

The proposed solution is to use solution 2 as it is the simplest solution and eliminates unnecessary expense.

Writing ORC/Parquet is an additional complication, the first cohort generates a 13m csv file, which doesnt warrant the addition effort in implementing ORC/Parquet

## Details

The CohortTableExportHandler exports the CohortItems to s3 here. NOTE: you need to be logged into the aws console using the profile Ophan - ETL job maintainer with access to Identity and Salesforce buckets' :

[https://s3.console.aws.amazon.com/s3/buckets/ophan-clean-price-migration-engine-cohort-items/data/?region=eu-west-1&tab=overview](https://s3.console.aws.amazon.com/s3/buckets/ophan-clean-price-migration-engine-cohort-items/data/?region=eu-west-1&tab=overview)

There is an airflow job that runs at 12:00-GMT, for details see:

[https://github.com/guardian/ophan-data-lake/blob/master/airflow/dags/supporter_experience.py](https://github.com/guardian/ophan-data-lake/blob/master/airflow/dags/supporter_experience.py)

There is a job that runs at 09:00-GMT that imports the above data and makes it available in the big query table:

```sql
datatech-platform-prod.datalake.price_migration_engine_cohort_items
```

This means there will currently be around a 24 hour delay in the data appearing in big query table.

This could be resolved by scheduling the price-migration-engine earlier in the day.  

You can access big query here:

[https://console.cloud.google.com/bigquery](https://console.cloud.google.com/bigquery)

You may need to request access to the tables, you can do so on the 'Data Tech' chat channel.