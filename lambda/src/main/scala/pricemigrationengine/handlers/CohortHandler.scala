package pricemigrationengine.handlers

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import pricemigrationengine.model._
import pricemigrationengine.services._
import ujson.Readable
import upickle.default.{read, stream}
import zio.console.Console
import zio.{ExitCode, Runtime, ZEnv, ZIO}

/**
  * A CohortHandler can be run as the handler of an AWS lambda or as a standalone program.
  */
trait CohortHandler extends zio.App with RequestStreamHandler {

  /**
    * Makes implementation available in lambda or console context.
    *
    * @param input CohortSpec with specification for the particular cohort that this handler is running over.
    * @param loggingService Implementation of Logging.Service, which will be context-specific.
    *                       Eg. if running in a lambda or console.
    * @return HandlerOutput if successful.
    */
  def handle(input: CohortSpec, loggingService: Logging.Service): ZIO[ZEnv, Failure, HandlerOutput]

  private def go(loggingService: Logging.Service, input: Readable): ZIO[ZEnv, Failure, HandlerOutput] =
    (for {
      cohortSpec <- validSpec(loggingService, input)
      output <- handle(cohortSpec, loggingService)
    } yield output).tap(output => loggingService.info(s"Output: $output"))

  private def validSpec(loggingService: Logging.Service, input: Readable): ZIO[ZEnv, Failure, CohortSpec] =
    (for {
      cohortSpec <-
        ZIO
          .effect(read[CohortSpec](input))
          .bimap(e => InputFailure(s"Failed to parse json: $e"), Option(_))
          .filterOrElse(_.exists(CohortSpec.isValid))(spec => ZIO.fail(InputFailure(s"Invalid cohort spec: $spec")))
      validSpec <- ZIO.fromOption(cohortSpec).orElseFail(InputFailure("No input"))
    } yield validSpec).tapBoth(
      e => loggingService.error(e.toString),
      spec => loggingService.info(s"Input: $spec")
    )

  final def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val loggingService = ConsoleLogging.service(Console.Service.live)
    (for {
      input <-
        ZIO
          .fromOption(args.headOption)
          .orElseFail(InputFailure("No input"))
          .tapError(e => loggingService.error(e.toString))
      _ <- go(loggingService, input)
    } yield ()).exitCode
  }

  final def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit =
    Runtime.default.unsafeRun {
      for {
        handlerOutput <- go(loggingService = LambdaLogging.service(context), input)
        writable <- ZIO.effect(stream(handlerOutput))
        _ <- ZIO.effect(writable.writeBytesTo(output))
      } yield ()
    }
}
