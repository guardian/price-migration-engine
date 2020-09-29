[![Known Vulnerabilities](https://snyk.io/test/github/guardian/price-migration-engine/badge.svg?targetFile=build.sbt)](https://snyk.io/test/github/guardian/price-migration-engine?targetFile=build.sbt)
# Price Migration Engine

This repo consists of a set of lambdas and DynamoDB table templates that are designed to interact together in a state 
machine.  

:warning: For production troubleshooting, see the [Troubleshooting](troubleshooting.md) doc :warning:

To set up a new cohort of subscriptions for price rise, see [this doc](cohort-setup.md).

See readme for:  
* [State machine](stateMachine/README.md)
* [Lambdas](lambda/README.md)
* [Data stores](dynamoDb/README.md)
