
#Running DynamoDB Locally

Install and run dynamoDB on you local machine using the following instructions:
- [https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.DownloadingAndRunning.html](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.DownloadingAndRunning.html)


Create the CohortTable using the following aws cli command:
```$bash
aws dynamodb create-table \
    --region eu-west-1 \
    --endpoint-url http://localhost:8000 \
    --table-name PriceMigrationEngineDEV \
    --attribute-definitions AttributeName=subscriptionNumber,AttributeType=S AttributeName=processingStage,AttributeType=S \
    --key-schema AttributeName=subscriptionNumber,KeyType=HASH \
    --global-secondary-indexes IndexName=ProcessingStageIndexV2,KeySchema=["{AttributeName=processingStage,KeyType=HASH}"],Projection="{ProjectionType=KEYS_ONLY}",ProvisionedThroughput="{ReadCapacityUnits=10,WriteCapacityUnits=10}" \
    --provisioned-throughput ReadCapacityUnits=10,WriteCapacityUnits=10 
```

Populate the cohort table:
```$bash
for i in {1..200} 
do 
echo "Inserting sub-$i"
aws dynamodb put-item \
    --region eu-west-1 \
    --endpoint-url http://localhost:8000 \
    --table-name PriceMigrationEngineDEV \
    --item "{\"subscriptionNumber\":{\"S\":\"sub-$i\"},\"processingStage\":{\"S\":\"ReadyForEstimation\"}}"
done || exit 1
```

Get the contents of the cohort table:
```$bash
aws dynamodb query \
    --region eu-west-1 \
    --endpoint-url http://localhost:8000 \
    --table-name PriceMigrationEngineDEV \
    --index-name ProcessingStageIndexV2 \
    --key-condition-expression "processingStage = :stage" \
    --expression-attribute-values '{":stage":{"S":"ReadyForEstimation"}}'
```

Get an entry in the cohort table by subscription id:
```$bash
aws dynamodb query \
    --region eu-west-1 \
    --endpoint-url http://localhost:8000 \
    --table-name PriceMigrationEngineDEV \
    --key-condition-expression "subscriptionNumber = :id" \
    --expression-attribute-values '{":id":{"S":"390493"}}'
```


Delete the cohort table:
```$bash
aws dynamodb delete-table \
    --region eu-west-1 \
    --endpoint-url http://localhost:8000 \
    --table-name PriceMigrationEngineDEV \
```

Update and item in the cohort table:
```$bash
aws dynamodb update-item \
    --region eu-west-1 \
    --endpoint-url http://localhost:8000 \
    --table-name PriceMigrationEngineDEV \
    --key '{"subscriptionNumber":{"S":"A-S00063981"}}' \
    --update-expression "SET processingStage = :stage" \
    --expression-attribute-values '{":stage":{"S":"EstimationComplete"}}'
```

Describe the cohort table:
```
aws dynamodb describe-table \
    --region eu-west-1 \
    --endpoint-url http://localhost:8000 \
    --table-name PriceMigrationEngineDEV 
```

Get a count of items in the cohort table:
```
aws dynamodb scan \
    --region eu-west-1 \
    --endpoint-url http://localhost:8000 \
    --table-name PriceMigrationEngineDEV \
    --select "COUNT"
```

##Modifying Global Secondary Indexes

Once created Global seconday indexes such as ProcessingStageIndexVxx cannot be modified. If you need to change the 
configuration of an index you can create a new one and delete the existing one. 

There is an additional limitation in that you can only do one Global Secondary Index addition or deletion 
(on an existing table) per cloudformation 'session'

To make a change to a Global Secondary Index follow the following procedure:
- Make a copy of the existing index configuration in the cloudformation template, incrementing the version number
in the name, and make the modifications to the configuration you require.
- Update the code in lambdas so they reference the new index name
- Once built, deploy the dynamodb configuration using riff-raff (MemSub::Subscriptions::DynamoDb::PriceMigrationEngine)
- Deploy the lambdas using riff-raff (MemSub::Subscriptions::Lambda::PriceMigrationEngine)
- Remove the old version of the index from the cloudformation template
- Once built, deploy the dynamodb config using riff-raff

    
