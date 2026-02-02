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
writeStream.write('productId,regionCode,currency,oldPrice,newPrice,pcIncrease\n');
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
  /*
    price example:
    {
      "currencyCode": "AED",
      "units": "36",
      "nanos": 690000000
    }
  */
};

const buildPrice_20260202 = (
  regionCode: string,
  currency: string,
  price: number,
): androidpublisher_v3.Schema$Money => {
  const [units, nanos] = price.toFixed(2).split('.');

  // The Bulgarian override:
  // From Google we get
  /*
    {
      "regionCode": "BG",
      "newSubscriberAvailability": true,
      "price": {
        "currencyCode": "EUR",
        "units": "9",
        "nanos": 250000000
      }
    }

  and the natural transform is

    {
      "regionCode": "BG",
      "newSubscriberAvailability": true,
      "price": {
        "currencyCode": "EUR",
        "units": "17",
        "nanos": 50000000
      }
    }

    But we want to change the currency to BGN

    {
      "regionCode": "BG",
      "newSubscriberAvailability": true,
      "price": {
        "currencyCode": "BGN",
        "units": "17",
        "nanos": 50000000
      }
    }

  */

  if (regionCode === "BG") {
    currency = "BGN"
  }

  return {
    currencyCode: currency,
    units: units,
    nanos: parseInt(`${nanos}0000000`),
  };
  /*
    price example:
    {
      "currencyCode": "AED",
      "units": "36",
      "nanos": 690000000
    }
  */
};

/**
 * Fetch existing basePlan from google API.
 * This is because we have to send the entire basePlan object in the PATCH request later
 */
const getProductIdCurrentBasePlan = async (
  client: androidpublisher_v3.Androidpublisher,
  packageName: string,
  productId: string,
): Promise<androidpublisher_v3.Schema$BasePlan> => {
  const resp = await client.monetization.subscriptions.get({
    packageName,
    productId,
  });

  if ((resp.data.basePlans?.length ?? 0) > 1) {
    console.log(`Base plan for ${productId} has ${resp.data.basePlans?.length} base plans`);
  }

  const bp = resp.data.basePlans?.[0];

  if (!bp) {
    throw new Error('No base plan found');
  }

  return bp;
};

type RegionalConfig = NonNullable<
  androidpublisher_v3.Schema$BasePlan['regionalConfigs']
>[number];

const updateRegionalConfig = (
  regionalConfig: RegionalConfig,
  googleRegionPriceMap: RegionPriceMap,
  productId: string,
): RegionalConfig => {

  // console.log(`regionalConfig: ${JSON.stringify(regionalConfig)}`);
  /*
    regional config example:
    {
      "regionCode": "CO",
      "newSubscriberAvailability": true,
      "price": {
        "currencyCode": "COP",
        "units": "39195",
        "nanos": 110000000
      }
    }
  */

  if ( !regionalConfig.regionCode || !googleRegionPriceMap[regionalConfig.regionCode] ) {
    return regionalConfig;
  }

  if (!regionsThatAllowOptOut.has(regionalConfig.regionCode)) {
    console.log(`Skipping region that doesn't allow opt-outs: ${regionalConfig.regionCode}`);
    return regionalConfig;
  }

  const priceDetails = googleRegionPriceMap[regionalConfig.regionCode];

  if (regionalConfig.price?.currencyCode !== priceDetails.currency) {
    console.log(
      `Currency mismatch for ${productId} in ${regionalConfig.regionCode}: ${regionalConfig.price?.currencyCode} -> ${priceDetails.currency}`,
    );
  }

  const currency = regionalConfig.price?.currencyCode ?? priceDetails.currency;

  const currentPrice = `${regionalConfig.price?.units ?? 0}.${
    regionalConfig.price?.nanos?.toString().slice(0, 2) ?? '00'
  }`;

  const pcIncrease = (priceDetails.price - parseFloat(currentPrice)) / parseFloat(currentPrice);

  writeStream.write(
    `${productId},${regionalConfig.regionCode},${currency},${currentPrice},${priceDetails.price},${pcIncrease}\n`,
  );

  return {
    ...regionalConfig,
    price: buildPrice_20260202(
      regionalConfig.regionCode,
      currency,
      priceDetails.price,
    ),
  };
};

// Returns a new BasePlan with updated prices
const updatePrices = (
  basePlan: androidpublisher_v3.Schema$BasePlan,
  googleRegionPriceMap: RegionPriceMap,
  productId: string,
): androidpublisher_v3.Schema$BasePlan => {
  const updatedRegionalConfigs = basePlan.regionalConfigs?.map(
    (regionalConfig) => updateRegionalConfig(regionalConfig, googleRegionPriceMap, productId),
  );

  return {
    ...basePlan,
    regionalConfigs: updatedRegionalConfigs,
  };
};

async function main() {
  const client = await getClient();

  for (const [productId, regionPriceMap] of Object.entries(priceRiseData)) {

    console.log(
      `Updating productId ${productId} in ${
        Object.keys(regionPriceMap).length
      } regions`,
    );

    const currentBasePlan = await getProductIdCurrentBasePlan(
      client,
      packageName,
      productId,
    );

    console.log(
      `productId ${productId} currentBasePlan: ${JSON.stringify(
        currentBasePlan,
      )}`,
    );

    const updatedBasePlan = updatePrices(
      currentBasePlan,
      regionPriceMap,
      productId,
    );

    if (!DRY_RUN) {
      console.log(
        `productId ${productId} updatedBasePlan: ${JSON.stringify(
          updatedBasePlan,
        )}`,
      );

      await client.monetization.subscriptions.patch({
        packageName,
        productId,
        'regionsVersion.version': '2025/01',
        updateMask: 'basePlans',
        requestBody: {
          productId,
          packageName,
          basePlans: [updatedBasePlan],
        },
      });

      console.log('Updated prices for', productId);
    }
  }  
}

main()
  .catch((err) => {
    console.log('Error:');
    console.log(err);
    process.exitCode = 1;
  })
  .finally(() => {
    writeStream.close();
  });
