## Android Price Rise

This directory contains scripts for performing an Android price migration.

We define a price migration as increasing the prices for new customers (this is sometimes called a "price increase"), and then increasing the prices of existing customers (sometimes called a "price rise")

The file `migration1.ts` performs the price increase, and the file `migration2.ts` performs the price rise. They must be ran in that order and under the instructions of Marketing.

Marketing will also provide the data file that describes how the prices should increase. This file is a .csv file with the following data

```
productName,country,currency,price
```

An example is

```
com.guardian.subscription.annual.14.freetrial,AU,AUD,169.99
com.guardian.subscription.annual.14.freetrial,CA,CAD,144.99
com.guardian.subscription.annual.14.freetrial,AL,USD,126.04
```

### How does it work ?

This scripts uses two Google API end points:

1. [monetization.subscriptions/patch](https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions/patch)
2. [monetization.subscriptions.basePlans/batchMigratePrices](https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions.basePlans/batchMigratePrices)

The first script, for instance, works as follows:

- It reads in a CSV file with the new prices for each product_id/region (see testPriceRise.csv for an example).
- For each product_id, it fetches the existing Rate Plan from the Google API. It then sends a modified version of this Rate Plan to the PATCH endpoint, based on the prices from the CSV.

The first script output a file at a location specified by the user.

Notes:

- When we ran the scripts for the Android price rise in Feb 2025, the `regionsVersion.version` needed to be updated from being `2022/02` to `2025/01`. It is possible that the version might need to be updated in the future. If the script fails, the error message will indicate what value should be used.

Note: There is an important difference between the two scripts. The first one can be run several times and sometimes you might have to do that if for instance the prices provided by Marketing are rejected (for being too high, or not having the right rounding, etc). But the second one cannot be reran after it has succeeded. Google doesn't allow the same product to be price rise within the same year.

### Preparations

To connect to the play store, the script will be using credentials stored in AWS Parameter Store. So give yourself the following Janus credentials.

```
account: mobile
path   : /mobile-purchase/android-subscription/google.serviceAccountJson2
```

Then make sure you install the dependencies

```
$ nvm use
$ yarn install
```

### Running the scripts

```
INPUT_FILE_PATH=/path/to/datafile.csv \
OUTPUT_FILE_PATH=/path/to/output.csv \
yarn android-price-migration-p1 --dry-run [--dry-run]

INPUT_FILE_PATH=/path/to/datafile.csv \
OUTPUT_FILE_PATH=/path/to/output.csv \
yarn android-price-migration-p2 --dry-run [--dry-run]
```

You can use the `--dry-run` parameter to check the changes before running with side effects.
