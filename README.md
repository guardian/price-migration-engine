[![Known Vulnerabilities](https://snyk.io/test/github/guardian/price-migration-engine/badge.svg?targetFile=build.sbt)](https://snyk.io/test/github/guardian/price-migration-engine?targetFile=build.sbt)

# Price Migration Engine

The price migration engine is an orchestration engine used to perform controlled price migrations. It currently consists in a collection of lambdas designed to work together as a state machine.

### General Introduction to price migrations and the engine:

- [An introduction to the general principles of price migrations](docs/price-migrations-from-first-principles.md)
- [The journey of a cohort item](docs/the-journey-of-a-cohort-item.md)
- [Coding directives](docs/coding-directives.md)
- [Notification windows](docs/notification-windows.md)
- [The art of computing start dates](docs/start-date-computation.md)
- [The art of the cap; or how to gracefully cap prices in the engine](docs/the-art-of-the-cap.md)

### Operations:

- [Set up a new cohort of subscriptions for price rise](docs/cohort-setup.md).
- [The migration implementation manual](docs/migration-implementation-manual.md)
- [Downloading fixtures](docs/downloading-fixtures.md)

### Further documentations:

* [State machine](stateMachine/README.md).
* [Lambdas](lambda/README.md).
* [Data stores](dynamoDb/README.md).

For production troubleshooting, see the [troubleshooting document](docs/troubleshooting.md)
