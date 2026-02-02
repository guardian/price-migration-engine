// Read the README.md for instructions on when and how to run this script.

import type { androidpublisher_v3 } from '@googleapis/androidpublisher';
import { regionsThatAllowOptOut } from './getRegionsThatAllowOptOut';
import { getClient } from './googleClient';
import { parsePriceRiseCsv } from './parsePriceRiseCsv';

const packageName = 'com.guardian';
// productId is like this 'com.guardian.subscription.annual.14.freetrial'
// basePlanId is like this 'p1m'

const dataFilePath = process.env.INPUT_FILE_PATH;
if (!dataFilePath) {
  console.log('Missing INPUT_FILE_PATH');
  process.exit(1);
}

const DRY_RUN = process.argv.includes('--dry-run');

const priceRiseData = parsePriceRiseCsv(dataFilePath);
/*
{
    "com.guardian.subscription.annual.14.freetrial": {
        "AU": {
            "price": 169.99,
            "currency": "AUD"
        },
        "CA": {
            "price": 144.99,
            "currency": "CAD"
        },
        (...)
        "US": {
            "price": 144.99,
            "currency": "USD"
        }
    },
    "com.guardian.subscription.monthly.11.freetrial": {
        "AU": {
            "price": 16.99,
            "currency": "AUD"
        },
        "CA": {
            "price": 14.99,
            "currency": "CAD"
        },
        "AL": {
            "price": 12.59,
            "currency": "USD"
        },
        (...)
*/

const getProductIdCurrentBasePlan = (
  client: androidpublisher_v3.Androidpublisher,
  packageName: string,
  productId: string,
): Promise<androidpublisher_v3.Schema$BasePlan> =>
  client.monetization.subscriptions
    .get({ packageName, productId })
    .then((resp) => {
      const bp = resp.data.basePlans ? resp.data.basePlans[0] : undefined;
      if (bp) {
        return bp;
      } else {
        return Promise.reject('No base plan found');
      }
    });

getClient()
  .then((client) => 
    Promise.all(
      // For each product_id in priceRiseData, update the prices in each region
      Object.entries(priceRiseData)
      .map(([productId, regionPriceMap]) => {

        // productId = 'com.guardian.subscription.annual.14.freetrial';
        /* 
          regionPriceMap = {        
              "AU": {
                  "price": 169.99,
                  "currency": "AUD"
              },
              "CA": {
                  "price": 144.99,
                  "currency": "CAD"
              },
              (...)
              "US": {
                  "price": 144.99,
                  "currency": "USD"
              }
        }
        */

        return getProductIdCurrentBasePlan(client, packageName, productId)
          .then((currentBasePlan: androidpublisher_v3.Schema$BasePlan) => {
            // https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions#BasePlan
            /*
              {
                  "basePlanId": "p1m",
                  "regionalConfigs": [
                      {
                          "regionCode": "AE",
                          "newSubscriberAvailability": true,
                          "price": {
                              "currencyCode": "AED",
                              "units": "47",
                              "nanos": 710000000
                          }
                      },
                      {
                          "regionCode": "ZM",
                          "newSubscriberAvailability": true,
                          "price": {
                              "currencyCode": "USD",
                              "units": "12",
                              "nanos": 990000000
                          }
                      },
                      {
                          "regionCode": "ZW",
                          "newSubscriberAvailability": true,
                          "price": {
                              "currencyCode": "USD",
                              "units": "12",
                              "nanos": 990000000
                          }
                      }
                      (...)
                  ],
                  "state": "ACTIVE",
                  "autoRenewingBasePlanType": {
                      "billingPeriodDuration": "P1M",
                      "gracePeriodDuration": "P30D",
                      "resubscribeState": "RESUBSCRIBE_STATE_ACTIVE",
                      "prorationMode": "SUBSCRIPTION_PRORATION_MODE_CHARGE_ON_NEXT_BILLING_DATE",
                      "legacyCompatible": true,
                      "legacyCompatibleSubscriptionOfferId": "offer1month",
                      "accountHoldDuration": "P30D"
                  },
                  "otherRegionsConfig": {
                      "usdPrice": {
                          "currencyCode": "USD",
                          "units": "9",
                          "nanos": 940000000
                      },
                      "eurPrice": {
                          "currencyCode": "EUR",
                          "units": "9",
                          "nanos": 320000000
                      },
                      "newSubscriberAvailability": true
                  }
              }
            */

            const regionalPriceMigrationConfigs = currentBasePlan.regionalConfigs?.map((regionalConfig) => {

              /* 
                regionalConfig =
                {
                  "regionCode": "AE",
                  "newSubscriberAvailability": true,
                  "price": {
                      "currencyCode": "AED",
                      "units": "47",
                      "nanos": 710000000
                  }
                }
              */

              // And now we want to build a RegionalPriceMigrationConfig
              // https://developers.google.com/android-publisher/api-ref/rest/v3/RegionalPriceMigrationConfig

              // Required. Region code this configuration applies to, as defined by ISO 3166-2, e.g. "US".
              const regionCode = regionalConfig.regionCode;

              // Required. Subscribers in all legacy price cohorts before this time will be migrated to the 
              // current price. Subscribers in any newer price cohorts are unaffected. 
              // Affected subscribers will receive one or more notifications from Google Play about 
              // the price change. Price decreases occur at the subscriber's next billing date. 
              // Price increases occur at the subscriber's next billing date following a notification 
              // period that varies by region and price increase type.
              const oldestAllowedPriceVersionTime = (new Date).toISOString(); // We are defaulting to now.

              // priceIncreaseType
              const priceIncreaseType = "PRICE_INCREASE_TYPE_OPT_OUT";

              return {
                regionCode,
                oldestAllowedPriceVersionTime,
                priceIncreaseType
              }
            });

            /*
              regionalPriceMigrationConfigs = 
              [
                  {
                      "regionCode": "AE",
                      "oldestAllowedPriceVersionTime": "2025-03-05T00:00:00Z",
                      "priceIncreaseType": "PRICE_INCREASE_TYPE_OPT_OUT"
                  },
                  {
                      "regionCode": "AT",
                      "oldestAllowedPriceVersionTime": "2025-03-05T00:00:00Z",
                      "priceIncreaseType": "PRICE_INCREASE_TYPE_OPT_OUT"
                  },
                  {
                      "regionCode": "AU",
                      "oldestAllowedPriceVersionTime": "2025-03-05T00:00:00Z",
                      "priceIncreaseType": "PRICE_INCREASE_TYPE_OPT_OUT"
                  },
                  (...)

            */

            const regionalPriceMigrationConfigs2 = regionalPriceMigrationConfigs?.filter((regionalPriceMigrationConfig) => {
              if (regionalPriceMigrationConfig.regionCode) {
                return regionsThatAllowOptOut.has(regionalPriceMigrationConfig.regionCode);
              } else {
                return false
              }
            });

            const request = {
              packageName,
              productId,
              basePlanId: currentBasePlan.basePlanId,
              regionalPriceMigrations: regionalPriceMigrationConfigs2,
              regionsVersion: {
                version: '2025/01'
              },
            };

            // https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions.basePlans/batchMigratePrices#MigrateBasePlanPricesRequest
            const requests: androidpublisher_v3.Schema$MigrateBasePlanPricesRequest[] = [request];

            console.log('migrating', productId);

            if (!DRY_RUN) {
              // https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions.basePlans/migratePrices
              // https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions.basePlans/batchMigratePrices
              
              return client.monetization.subscriptions.basePlans.batchMigratePrices({
                packageName,
                productId,
                requestBody: {
                  requests: requests
                }
              }).then((response) => {
                console.log('response', response.data);
              });
            }
          });
      }),
    ),
  )
  .catch((err) => {
    console.log('Error:');
    console.log(err);
  });
