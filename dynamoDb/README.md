
#Running DynamoDB Locally

Install and run dynamoDB on you local machine using the following instructions:
- [https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.DownloadingAndRunning.html](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.DownloadingAndRunning.html)


Create the CohortTable using the following aws cli command:
```$bash
aws dynamodb create-table \
    --region eu-west-1 \
    --endpoint-url http://localhost:8000 \
    --table-name PriceMigrationEngineDev \
    --attribute-definitions AttributeName=subscriptionNumber,AttributeType=S AttributeName=processingStage,AttributeType=S \
    --key-schema AttributeName=subscriptionNumber,KeyType=HASH \
    --global-secondary-indexes IndexName=ProcessingStageIndex,KeySchema=["{AttributeName=processingStage,KeyType=HASH}"],Projection="{ProjectionType=KEYS_ONLY}",ProvisionedThroughput="{ReadCapacityUnits=10,WriteCapacityUnits=10}" \
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
    --table-name PriceMigrationEngineDev \
    --item "{\"subscriptionNumber\":{\"S\":\"sub-$i\"},\"processingStage\":{\"S\":\"ReadyForEstimation\"}}"
done || exit 1
```

Get the contents of the cohort table:
```$bash
aws dynamodb query \
    --region eu-west-1 \
    --endpoint-url http://localhost:8000 \
    --table-name PriceMigrationEngineDev \
    --index-name ProcessingStageIndex \
    --key-condition-expression "processingStage = :stage" \
    --expression-attribute-values '{":stage":{"S":"ReadyForEstimation"}}'
```

Delete the cohort table:
```$bash
aws dynamodb delete-table \
    --region eu-west-1 \
    --endpoint-url http://localhost:8000 \
    --table-name PriceMigrationEngineDev \
```

