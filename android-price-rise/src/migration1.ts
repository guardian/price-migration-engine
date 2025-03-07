// Read the README.md for instructions on when and how to run this script.

import fs from 'fs';
import type { androidpublisher_v3 } from '@googleapis/androidpublisher';
import { regionsThatAllowOptOut } from './getRegionsThatAllowOptOut';
import { getClient } from './googleClient';
import { parsePriceRiseCsv } from './parsePriceRiseCsv';
import type { RegionPriceMap } from './parsePriceRiseCsv';

const packageName = 'com.guardian';

const inputDataFilePath = process.env.INPUT_FILE_PATH;
if (!inputDataFilePath) {
  console.log('Missing INPUT_FILE_PATH');
  process.exit(1);
}

const outputFilePath = process.env.OUTPUT_FILE_PATH;
if (!outputFilePath) {
  console.log('Missing OUTPUT_FILE_PATH');
  process.exit(1);
}

const DRY_RUN = process.argv.includes('--dry-run');
const writeStream = fs.createWriteStream(outputFilePath);
writeStream.write(
  'productId,regionCode,currency,oldPrice,newPrice,pcIncrease\n',
);
if (DRY_RUN) {
  console.log('*****DRY RUN*****');
}

const priceRiseData = parsePriceRiseCsv(inputDataFilePath);

const buildPrice = (
  currency: string,
  price: number,
): androidpublisher_v3.Schema$Money => {
  const [units, nanos] = price.toFixed(2).split('.');
  return {
    currencyCode: currency,
    units: units,
    nanos: parseInt(`${nanos}0000000`),
  };
};

/**
 * Fetch existing basePlan from google API.
 * This is because we have to send the entire basePlan object in the PATCH request later
 */
const getProductIdCurrentBasePlan = (
  client: androidpublisher_v3.Androidpublisher,
  packageName: string,
  productId: string,
): Promise<androidpublisher_v3.Schema$BasePlan> =>
  client.monetization.subscriptions
    .get({ packageName, productId })
    .then((resp) => {
      if ((resp.data.basePlans?.length ?? 0) > 1) {
        console.log(
          `Base plan for ${productId} has ${resp.data.basePlans?.length} base plans`,
        );
      }
      const bp = resp.data.basePlans ? resp.data.basePlans[0] : undefined;
      if (bp) {
        return bp;
      } else {
        return Promise.reject('No base plan found');
      }
    });

// Returns a new BasePlan with updated prices
const updatePrices = (
  basePlan: androidpublisher_v3.Schema$BasePlan,
  googleRegionPriceMap: RegionPriceMap,
  productId: string,
): androidpublisher_v3.Schema$BasePlan => {
  const updatedRegionalConfigs = basePlan.regionalConfigs?.map(
    (regionalConfig) => {
      if (
        regionalConfig.regionCode &&
        googleRegionPriceMap[regionalConfig.regionCode]
      ) {
        if (!regionsThatAllowOptOut.has(regionalConfig.regionCode)) {
          console.log(
            `Skipping region that doesn't allow opt-outs: ${regionalConfig.regionCode}`,
          );
          return regionalConfig;
        }
        // Update the price
        const priceDetails = googleRegionPriceMap[regionalConfig.regionCode];
        if (regionalConfig.price?.currencyCode !== priceDetails.currency) {
          console.log(
            `Currency mismatch for ${productId} in ${regionalConfig.regionCode}: ${regionalConfig.price?.currencyCode} -> ${priceDetails.currency}`,
          );
        }
        const currency =
          regionalConfig.price?.currencyCode ?? priceDetails.currency;
        const currentPrice = `${regionalConfig.price?.units ?? 0}.${
          regionalConfig.price?.nanos?.toString().slice(0, 2) ?? '00'
        }`;
        const pcIncrease =
          (priceDetails.price - parseFloat(currentPrice)) /
          parseFloat(currentPrice);
        writeStream.write(
          `${productId},${regionalConfig.regionCode},${currency},${currentPrice},${priceDetails.price},${pcIncrease}\n`,
        );
        return {
          ...regionalConfig,
          price: buildPrice(currency, priceDetails.price),
        };
      } else {
        // No mapping for this product_id/region, don't change it
        return regionalConfig;
      }
    },
  );
  return {
    ...basePlan,
    regionalConfigs: updatedRegionalConfigs,
  };
};

getClient()
  .then((client) =>
    Promise.all(
      // For each product_id in priceRiseData, update the prices in each region
      Object.entries(priceRiseData).map(([productId, regionPriceMap]) => {
        console.log(
          `Updating productId ${productId} in ${
            Object.keys(regionPriceMap).length
          } regions`,
        );

        return getProductIdCurrentBasePlan(client, packageName, productId)
          .then((currentBasePlan) => {
            return updatePrices(currentBasePlan, regionPriceMap, productId);
          })
          .then((updatedBasePlan: androidpublisher_v3.Schema$BasePlan) => {
            if (!DRY_RUN) {
              // https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions/patch
              return client.monetization.subscriptions
                .patch({
                  packageName,
                  productId,
                  'regionsVersion.version': '2025/01',
                  updateMask: 'basePlans',
                  requestBody: {
                    productId,
                    packageName,
                    basePlans: [updatedBasePlan],
                  },
                })
                .then((response) => {
                  console.log('Updated prices for', productId);
                });
            }
          });
      }),
    ),
  )
  .catch((err) => {
    console.log('Error:');
    console.log(err);
  })
  .finally(() => {
    writeStream.close();
  });
