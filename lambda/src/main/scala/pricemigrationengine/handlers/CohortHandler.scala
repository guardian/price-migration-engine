package pricemigrationengine.handlers

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import pricemigrationengine.model._
import pricemigrationengine.services._
import ujson.Readable
import upickle.default.{read, stream}
import zio.{ExitCode, Runtime, ZEnv, ZIO}

/** A CohortHandler can be run as the handler of an AWS lambda or as a standalone program.
  */
trait CohortHandler extends zio.App with RequestStreamHandler {

  /** Makes implementation available in lambda or console context.
    *
    * @param input CohortSpec with specification for the particular cohort that this handler is running over.
    * @return HandlerOutput if successful.
    */
  def handle(input: CohortSpec): ZIO[ZEnv with Logging, Failure, HandlerOutput]

  private def go(input: Readable): ZIO[ZEnv with Logging, Failure, HandlerOutput] =
    (for {
      cohortSpec <- validSpec(input)
      output <- handle(cohortSpec)
    } yield output).tap(output => Logging.info(s"Output: $output"))

  private def validSpec(input: Readable): ZIO[Logging, Failure, CohortSpec] =
    (for {
      cohortSpec <-
        ZIO
          .effect(read[CohortSpec](input))
          .bimap(e => InputFailure(s"Failed to parse json: $e"), Option(_))
          .filterOrElse(_.exists(CohortSpec.isValid))(spec => ZIO.fail(InputFailure(s"Invalid cohort spec: $spec")))
      validSpec <- ZIO.fromOption(cohortSpec).orElseFail(InputFailure("No input"))
    } yield validSpec).tapBoth(
      e => Logging.error(e.toString),
      spec => Logging.info(s"Input: $spec")
    )

  final def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    (for {
      input <-
        ZIO
          .fromOption(args.headOption)
          .orElseFail(InputFailure("No input"))
          .tapError(e => Logging.error(e.toString))
      _ <- go(input)
    } yield ())
      .provideCustomLayer(ConsoleLogging.impl)
      .exitCode

  final def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit =
    Runtime.default.unsafeRun {
      for {
        handlerOutput <- go(input).provideCustomLayer(LambdaLogging.impl(context))
        writable <- ZIO.effect(stream(handlerOutput))
        _ <- ZIO.effect(writable.writeBytesTo(output))
      } yield ()
    }
}
