package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.{EstimationComplete}
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.console.Console
import zio.{App, Runtime, ZEnv, ZIO, ZLayer, console}

case class CreateSalesforcePriceRiseResult()

object SalesforcePriceRiseCreateHandler extends App with RequestHandler[Unit, Unit] {

  val main: ZIO[Logging with Configuration with CohortTable, Failure, Unit] =
    for {
      config <- Configuration.config
      cohortItems <- CohortTable.fetch(EstimationComplete, config.batchSize)
      _ <- cohortItems.foreach(createSalesforcePriceRise)
    } yield ()

  private def createSalesforcePriceRise(
    item: CohortItem
  ): ZIO[Logging with Configuration with CohortTable, Failure, Unit] =
    for {
      config <- Configuration.config
      result <- updateSalesforce(item)
        .tapBoth(
          e => Logging.error(s"Failed to write create Price_Rise in salesforce: $e"),
          result => Logging.info(s"Estimated result: $result")
        )
      _ <- CohortTable
        .update(result)
        .tapBoth(
          e => Logging.error(s"Failed to update Cohort table: $e"),
          _ => Logging.info(s"Wrote $result to Cohort table")
        )
    } yield ()

  private def updateSalesforce(cohortItem: CohortItem) = {
    ZIO
      .effect(CreateSalesforcePriceRiseResult())
      .mapError(_ => SalesforceFailure(""))
  }

  private def env(
      loggingLayer: ZLayer[Any, Nothing, Logging]
  ): ZLayer[Any, Any, Logging with Configuration with CohortTable with Zuora] = {
    val configLayer = EnvConfiguration.impl
    val cohortTableLayer =
      loggingLayer ++ configLayer >>>
        DynamoDBClient.dynamoDB ++ loggingLayer ++ configLayer >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ configLayer >>>
        CohortTableLive.impl
    val zuoraLayer =
      configLayer ++ loggingLayer >>>
        ZuoraLive.impl
    loggingLayer ++ configLayer ++ cohortTableLayer ++ zuoraLayer
  }

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideSomeLayer(
        env(Console.live >>> ConsoleLogging.impl)
      )
      // output any failures in service construction - there's probably a better way to do this
      .foldM(
        e => console.putStrLn(s"Failed: $e") *> ZIO.succeed(1),
        _ => console.putStrLn("Succeeded!") *> ZIO.succeed(0)
      )

  def handleRequest(unused: Unit, context: Context): Unit =
    runtime.unsafeRun(
      main.provideSomeLayer(
        env(LambdaLogging.impl(context))
      )
    )
}
