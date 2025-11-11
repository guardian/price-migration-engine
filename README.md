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
- [Operational Directives](docs/operational-directives.md)

### Future price migrations special directives

- In late 2026, there will be the next Supporter Plus price migration. To avoid the billing date mis-alignment problem we encountered in 2025, there should be a product re-structuration performed as part of the price migration to align the billing dates of the main charge and the extra contribution. Note that we also have a [permanent stop](https://github.com/guardian/price-migration-engine/pull/1307) to prevent processing such rate plans across all products.

### Web Price Rises

- [Notification windows](docs/notification-windows.md)
- [The art of computing amendment effective dates](docs/amendment-effective-date-computation.md)
- [The art of the cap; or how to gracefully cap prices in the engine](docs/the-art-of-the-cap.md)
- [What does ROW (Rest of World) means ?](docs/ROW-definition.md)
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
- [Quirks of the Engine](docs/quirks.md)

### Android Price Rises

- [README](./android-price-rise/README.md)

### Tools:

See [tools/README.md](tools/README.md)

### Further documentations:

* [State machine](stateMachine/README.md)
* [Lambdas](lambda/README.md)
* [Data stores](dynamoDb/README.md)
