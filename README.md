[![Known Vulnerabilities](https://snyk.io/test/github/guardian/price-migration-engine/badge.svg?targetFile=build.sbt)](https://snyk.io/test/github/guardian/price-migration-engine?targetFile=build.sbt)

# Price Migration Engine

The price migration engine is an orchestration engine used to perform controlled price migrations. 

It currently consists in

- A collection of lambdas designed to work together as a state machine
- The TypeScript code required to run an Android price rise

### General Introduction to price migrations and the engine:

- [An introduction to the general principles of price migrations](docs/price-migrations-from-first-principles.md)
- [The journey of a cohort item](docs/the-journey-of-a-cohort-item.md)
- [Coding directives](docs/coding-directives.md)
- [Notification windows](docs/notification-windows.md)
- [The art of computing amendment effective dates](docs/amendment-effective-date-computation.md)
- [The art of the cap; or how to gracefully cap prices in the engine](docs/the-art-of-the-cap.md)

### Operations:

- Web Price Rises
    - [lambdas code structure](docs/lambdas-code-structure.md)
    - [Set up a new cohort of subscriptions for price rise](docs/subscription-numbers-upload.md)
    - [Setting migration extra attributes](docs/migration-extra-attributes.md)
    - [The migration implementation manual](docs/migration-implementation-manual.md)
    - [Downloading fixtures](docs/downloading-fixtures.md)
    - [Notes on the Zuora Order API](docs/zuora-order-api.md)
    - [Communication with braze](docs/communication-with-braze.md)
    - [Notes on prices](docs/notes-on-prices.md)
    - [Cohort Items](docs/cohort-items.md)
    - [Troubleshooting document](docs/troubleshooting.md)

- Android Price Rises
    - [README](./android-price-rise/README.md)


### Further documentations:

* [State machine](stateMachine/README.md)
* [Lambdas](lambda/README.md)
* [Data stores](dynamoDb/README.md)
