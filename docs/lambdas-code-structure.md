## Price Migration Engine - Lambdas

### Code structure

The codebase is structured to keep a clear separation between [pure](https://docs.scala-lang.org/scala3/book/fp-pure-functions.html) and [effectful](https://en.wikipedia.org/wiki/Side_effect_(computer_science)) code. (*Effectful* has different meanings, but I mean by it having side-effects or not pure.)

The [model](../lambda/src/main/scala/pricemigrationengine/model) package holds all the pure code in the project. All code here is deterministic. It neither generates nor depends on any kind of real-world effect; including random numbers, relative dates, logging or printing to console.

The effects of the code are generated and consumed through [services](../lambda/src/main/scala/pricemigrationengine/services), following the [ZIO convention](https://zio.dev). Each service has a type, an interface and at least one implementation. For example, the [Zuora type](../lambda/src/main/scala/pricemigrationengine/services/Zuora.scala), and
[live implementation](../lambda/src/main/scala/pricemigrationengine/services/ZuoraLive.scala).

These services are composed together into ZIO vertical and horizontal [layers](https://github.com/zio/zio/blob/master/docs/datatypes/zlayer.md), and these layers form the runtime environment for each of the lambdas by compile-time dependency injection. A vertical layer is one in which one service depends on another: they are related together by the `>>>` operator. In a horizontal layer, two peer services are related together by the `++` operator. For a more detailed explanation of how these layers work, see the [ZIO documentation](https://zio.dev/reference/contextual/zlayer/).

The lambdas are all in the [handlers](../lambda/src/main/scala/pricemigrationengine/handlers) package.

All the [dependencies](../project/Dependencies.scala) of the project have been chosen for their light weight and minimal number of transitive dependencies, so that the artefact generated is of minimal size and lambdas can warm up quickly.

The same generated jar is used by all the lambdas. The only variation in their deployment is the configuration of the
main endpoint.

### To run lambdas locally in Intellij

You can run or debug any of the lambdas in any deployment environment from Intellij.  
You will need up-to-date AWS credentials stored locally.
Set up a run configuration for the lambda, using the following environment variables:

* AWS_PROFILE=`profileName`
* stage=`DEV|CODE|PROD`

and also the specific environment variables for the lambda you are running.

### Environment variables

The environment variables set in the lambdas are provided by [Secrets Manager](https://eu-west-1.console.aws.amazon.com/secretsmanager/secret?name=price-migration-engine-lambda-PROD&region=eu-west-1) and can also be changed manually in the AWS console by navigating to the lambda (e.g.: price-migration-engine-estimation-lambda-CODE) > Configuration > Environment variables

_Note:_ The DEV and CODE environments both currently point to `apisandbox.zuora.com`.

We have a copy of our production Zuora environment, the "centralsandbox" (which we access via `test.zuora.com`). However, as we do not have an equivalent salesforce environment set up, we cannot run the step function from start to finish with this environment.

Individual lambdas like the `EstimationLambda` and the `AmendmentLambda` can be run against this Zuora environment if we want to test on subscriptions in production. See how to [run the lambdas locally](#to-run-lambdas-locally-in-intellij) with the necessary environment variables (see above).

### Environment variables for EstimationHandler and Amendment Handler

* input=`The input cohort spec in JSON string format` (see below for example)
* batchSize=`batchSize`
* zuoraApiHost=`host`
* zuoraClientId=`personal clientId`
* zuoraClientSecret=`personal clientSecret`

### Input Cohort Spec Example

``` 
{
  "cohortName": "EchoLegacyTesting",
  "brazeName": "cmp123",
  "earliestAmendmentEffectiveDate": "2022-08-02"
}
```

Note the above is different to the input JSON that the lambda admits 
