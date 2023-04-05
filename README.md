[![Known Vulnerabilities](https://snyk.io/test/github/guardian/price-migration-engine/badge.svg?targetFile=build.sbt)](https://snyk.io/test/github/guardian/price-migration-engine?targetFile=build.sbt)

# Price Migration Engine

The price migration engine is an orchestration engine used to perform controlled price migrations. It currently consists in a collection of lambdas designed to work together as a state machine.

To set up a new cohort of subscriptions for price rise, see [cohort setup](docs/cohort-setup.md).

See readme for:

* [State machine](stateMachine/README.md).
* [Lambdas](lambda/README.md).
* [Data stores](dynamoDb/README.md).
* [Notification periods](docs/notification-periods.md).

For production troubleshooting, see the [troubleshooting document](docs/troubleshooting.md)
