## Android Price Rise

This directory contains a script for performing an Android price rise.

Once a price increase has been done, we can migrate (price rise, price migration) existing subscribers to the new price from the Google Play web console.

To connect to the play store, the script will be using credentials stored in AWS Parameter Store.

```
account: mobile
path   : /mobile-purchase/android-subscription/google.serviceAccountJson2
```

### Preparations

Run:

```
$ nvm use
$ yarn install
```

Then, give yourself Janus credentials for the mobile account.

### How does it work ?

This script uses the [PATCH endpoint](https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions/patch) to change the prices.

It works as follows:

1. It reads in a CSV file with the new prices for each product_id/region (see testPriceRise.csv for an example).
2. For each product_id, it fetches the existing Rate Plan from the Google API. It then sends a modified version of this Rate Plan to the PATCH endpoint, based on the prices from the CSV.

The script outputs a CSV file listing every product_id/region that was updated.

### Running the script

```
FILE_PATH=/path/to/price-rise.csv yarn android-price-rise [--dry-run]
```

Use the `--dry-run` parameter to check the changes before running the script for real.
