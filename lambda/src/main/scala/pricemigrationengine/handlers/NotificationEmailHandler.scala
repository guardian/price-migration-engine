package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.Context
import pricemigrationengine.model.Failure
import pricemigrationengine.services._
import zio.console.Console
import zio.random.Random
import zio.{Runtime, ZEnv, ZIO, ZLayer, console}

class NotificationEmailHandler {
  val main: ZIO[Logging with CohortTable with SalesforceClient, Failure, Unit] = ???

  private def env(
    loggingLayer: ZLayer[Any, Nothing, Logging]
  ): ZLayer[Any, Any, Logging with CohortTable with SalesforceClient] = {
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++
          EnvConfiguration.stageImp ++ EnvConfiguration.salesforceImp >>>
        CohortTableLive.impl ++ SalesforceClientLive.impl
    val zuoraLayer =
      EnvConfiguration.zuoraImpl ++ loggingLayer >>>
        ZuoraLive.impl
    loggingLayer ++ EnvConfiguration.amendmentImpl ++ cohortTableLayer ++ zuoraLayer ++ Random.live
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
