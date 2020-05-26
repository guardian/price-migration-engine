package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import pricemigrationengine.model.CohortTableFilter.SalesforcePriceRiceCreationComplete
import pricemigrationengine.model._
import pricemigrationengine.services._
import zio.console.Console
import zio.{App, Runtime, ZEnv, ZIO, ZLayer, console}

object SubscriptionIdUploadHandler extends App with RequestHandler[Unit, Unit] {

  val main: ZIO[Logging with CohortTable, Failure, Unit] = ???

  private def env(
      loggingLayer: ZLayer[Any, Nothing, Logging]
  ) = {
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
      DynamoDBClient.dynamoDB ++ loggingLayer >>>
      DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp >>>
      CohortTableLive.impl
    loggingLayer ++ EnvConfiguration.amendmentImpl ++ cohortTableLayer
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
