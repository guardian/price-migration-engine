# Price Migration Engine

This repo consists of a set of lambdas and DynamoDB table templates that are designed to interact together in a state 
machine.  
The lambdas are all in the [handlers](lambda/src/main/scala/pricemigrationengine/handlers) package.

## To run lambdas locally in Intellij
You can run or debug any of the lambdas in any deployment environment from Intellij.  
You will need up-to-date AWS credentials stored locally.  
Set up a run configuration for the lambda, using the following environment variables:
* AWS_PROFILE=`profileName`
* stage=`DEV|CODE|PROD`  

and also the specific environment variables for the lambda you are running.

### Specific environment variables per lambda

#### EstimationHandler
* earliestStartDate=`earliestStartDate`
* batchSize=`batchSize`
* zuoraApiHost=`host`
* zuoraClientId=`personal clientId`
* zuoraClientSecret=`personal clientSecret`

#### AmendmentHandler
* earliestStartDate=`earliestStartDate`
* batchSize=`batchSize`
* zuoraApiHost=`host`
* zuoraClientId=`personal clientId`
* zuoraClientSecret=`personal clientSecret`

## Importing subscription id for a price migration

See [ImportSubscriptionId.MD](ImportSubscriptionId.MD)
