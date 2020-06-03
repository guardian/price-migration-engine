package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.Context
import pricemigrationengine.model.CohortTableFilter.AmendmentComplete
import pricemigrationengine.model.{Failure, NotificationEmailFailure}
import pricemigrationengine.services._
import zio.clock.Clock
import zio.console.Console
import zio.{Runtime, ZEnv, ZIO, ZLayer, clock, console}

object NotificationEmailHandler {
  private val NotificationEmailLeadTimeDays = 30

  val main: ZIO[Logging with CohortTable with SalesforceClient with Clock, Failure, Unit] = {
    for {
      now <- clock.currentDateTime.mapError(ex => NotificationEmailFailure(s"Failed to get time: $ex"))
      subscriptions <- CohortTable.fetch(
        AmendmentComplete, Some(now.toLocalDate.plusDays(NotificationEmailLeadTimeDays))
      )
      _ <- subscriptions.foreach { subscription =>
        Logging.info(s"Sending notification email for subscription: ${subscription.subscriptionName}")
      }
    } yield ()
  }

  private def env(
    loggingLayer: ZLayer[Any, Nothing, Logging]
  ): ZLayer[Any, Any, Logging with CohortTable with SalesforceClient with Clock] = {
    val cohortTableLayer =
      loggingLayer ++ EnvConfiguration.dynamoDbImpl >>>
        DynamoDBClient.dynamoDB ++ loggingLayer >>>
        DynamoDBZIOLive.impl ++ loggingLayer ++ EnvConfiguration.cohortTableImp ++
          EnvConfiguration.stageImp ++ EnvConfiguration.salesforceImp >>>
        CohortTableLive.impl ++ SalesforceClientLive.impl ++ Clock.live
    loggingLayer ++ cohortTableLayer
  }

  private val runtime = Runtime.default

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    main
      .provideSomeLayer(
        env(Console.live >>> ConsoleLogging.impl)
      )
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
