# Downloading fixtures

This chapter explain the way to download and sanitize test fixtures from Zuora.

### Tests types

The engine as essentially three types of tests. 

1. Unit tests when we are testing specific (often) pure functions

2. Simulated integration tests. (The engine doesn't connect to AWS S3, Zuora or Salesforce during tests, and instead simulate talking to those systems using mock interfaces)

3. Tests that require data from Zuora, more exactly bits of the Zuora data model (subscriptions, accounts, invoice previews and the product catalogue) that need to be deserialised into types of the engine and used in functions. They are particularly important when setting up a new migration because they help ensuring that the functions required for Estimation and Amendment for that migration return the right values. Those check are very important.


### Test fixtures

Test fixtures are put in the directory `price-migration-engine/lambda/src/test/resources`. That directory as a bit of a structure, but not extremelly strongly enforced, so do what makes sense. For isntance the directory

```
price-migration-engine/lambda/src/test/resources/DigiSubs2023/annual
```

contains the files related to an annual subscription for the `DigiSubs2023` migration. We have a library

```
import pricemigrationengine.Fixtures
```

that we use for loading and unserializing these files. For instance the file

```
price-migration-engine/lambda/src/test/resources/DigiSubs2023/annual/subscription.json
```

will be deserialised in to subscription with 

```
Fixtures.subscriptionFromJson("DigiSubs2023/annual/subscription.json")
```

### Zuora data download

Not all 4 files that we are going to download in this section are required for every test, but they are the files all tests use. In this section we will be assuming that you already have a way to retrieve a new Zuora bearer token. They are valid for one hour. (Pascal, for instance, has it own Zuora client id and client secret that are being used in a HTTP call to retrieve a new token)

Let's us then assume that you have the valid access token 

```
access token: "bb8b6d099d7fcb139b844d18a80cbf6b"
```

Let us also assume that you are interested in subscription `A-00000012`

### Getting the subscription

The first file you are going to downlaod is the subscription itself 

The curl for that is 

```
curl -X GET \
     -H "Authorization: Bearer bb8b6d099d7fcb139b844d18a80cbf6b" \
     -H "Content-Type: application/json" \
     "https://rest.zuora.com/v1/subscriptions/A-00000012"
```

and the general pattern is 

```
curl -X GET \
     -H "Authorization: Bearer <zuora token>" \
     -H "Content-Type: application/json" \
     "https://rest.zuora.com/v1/subscriptions/<subsccription number>"
```

The subscription will be printed to the standard output (your terminal), just redirect to the file `subscription.json`. It should be pretty printed.


### Getting the account

Now that we have the subscription, from the subscription we read the attribute `accountId`. Let's assume that the value is `290527a8792a734ad7658cc4a3b686ff`

The curl for the account is

```
curl -X GET \
     -H "Authorization: Bearer bb8b6d099d7fcb139b844d18a80cbf6b" 
     -H "Content-Type: application/json" "https://rest.zuora.com/v1/accounts/290527a8792a734ad7658cc4a3b686ff"
```

and the general pattern is 

```
curl -X GET \
     -H "Authorization: Bearer <zuora token>" \
     -H "Content-Type: application/json" "https://rest.zuora.com/v1/accounts/<account-id>"
```

Note that instead of the account id, you can also use the account number.

The account information will be printed to the standard output (your terminal), just redirect to the file `account.json`. It should be pretty printed.

### Getting the invoice schedule

The invoice schedule is used by the engine code to determine the data of the next billing events.

The curl requires to specify a date in the future. You should use the current date plus at least 13 months (you can use 13 months). This is to ensure that if the subscription is an annual subscription then you will have at least one billing event in the preview. 

Assuming that today is 12th March 2024 (2024-03-12), then 13 months from now is 12th April 2025 (2025-04-12).

You will also need the account id. We have already used it, it's `290527a8792a734ad7658cc4a3b686ff`

The curl for the invoice preview is then

```
curl -X POST \
     -H "Authorization: Bearer bb8b6d099d7fcb139b844d18a80cbf6b" \
     -H "Content-Type: application/json" \
     -d '{"accountId": "290527a8792a734ad7658cc4a3b686ff", "targetDate": "2025-04-12", "assumeRenewal": "Autorenew", "chargeTypeToExclude": "OneTime"}' \
     "https://rest.zuora.com/v1/operations/billing-preview"
```

Note how unlike the previous cases, here we use a POST. That is because Zuora is going to synchronously compute the data for you before returning it for you.


The general pattern is 

```
curl -X POST \
     -H "Authorization: Bearer <zuora token>" \
     -H "Content-Type: application/json" \
     -d '{"accountId": "<account id>", "targetDate": "<target date>", "assumeRenewal": "Autorenew", "chargeTypeToExclude": "OneTime"}' \
     "https://rest.zuora.com/v1/operations/billing-preview"
```

The schedule will be printed to the standard output (your terminal), just redirect to the file `invoice-preview.json`. It should be pretty printed.

### The product catalogue 

The last file we use is the zuora producr catalogue. It was actually used in in the test of legacy code because legacy code was using the product catalogue to retrieve information that we are now hardcoding in migration specific code.

The curl is

```
curl -X GET \
    -H "Authorization: Bearer bb8b6d099d7fcb139b844d18a80cbf6b" \
    -H "Content-Type: application/json" "https://rest.zuora.com/v1/catalog/products"
``` 

and the general pattern is 

```
curl -X GET \
    -H "Authorization: Bearer <zuora token>" \
    -H "Content-Type: application/json" "https://rest.zuora.com/v1/catalog/products"
``` 

The catalogue will be printed to the standard output (your terminal), just redirect to the file `subscription.json`. It should be pretty printed.

### How many subcriptions should we download ?

Depends on what need to be tested. Another answer is that it depends on how many subtle variations of subscription you have in your migrations. For instance the Newspaper2024 migration uses 10 different subscription in its tests. That was because it was a multi product migration, with novel functionalities and special features and each variation of product and billing period needed to be properly tested. The GW2024 migration needs 4 subscriptions. A standard one, one where the extended currenty is "ROW (USD)" (USD paying subscriptions not in the United States), one where the price capping is non trivial, and one where the new 1 year policy is non trivial.

 