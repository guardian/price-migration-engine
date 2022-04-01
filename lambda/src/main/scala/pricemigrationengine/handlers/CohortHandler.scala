package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import pricemigrationengine.model._
import pricemigrationengine.services._
import ujson.Readable
import upickle.default.{read, stream}
import zio.{Runtime, ZEnv, ZIO, ZIOAppArgs, ZIOAppDefault}

import java.io.{InputStream, OutputStream}

/** A CohortHandler can be run as the handler of an AWS lambda or as a standalone program.
  */
trait CohortHandler extends ZIOAppDefault with RequestStreamHandler {

  /** Makes implementation available in lambda or console context.
    *
    * @param input
    *   CohortSpec with specification for the particular cohort that this handler is running over.
    * @return
    *   HandlerOutput if successful.
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
          .attempt(read[CohortSpec](input))
          .mapBoth(e => InputFailure(s"Failed to parse json: $e"), Option(_))
          .filterOrElseWith(_.exists(CohortSpec.isValid))(spec => ZIO.fail(InputFailure(s"Invalid cohort spec: $spec")))
      validSpec <- ZIO.fromOption(cohortSpec).orElseFail(InputFailure("No input"))
    } yield validSpec).tapBoth(
      e => Logging.error(e.toString),
      spec => Logging.info(s"Input: $spec")
    )

  override final def run: ZIO[ZEnv with ZIOAppArgs, Any, Any] =
    (for {
      inputFromEnv <- zio.System.env("input")
      input <- ZIO
        .fromOption(inputFromEnv)
        .orElseFail(InputFailure("No input"))
        .tapError(e => Logging.error(e.toString))
      _ <- go(input)
    } yield ())
      .provideCustomLayer(ConsoleLogging.impl)
      .exitCode

  override final def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit =
    Runtime.default.unsafeRun {
      for {
        handlerOutput <- go(input).provideCustomLayer(LambdaLogging.impl(context))
        writable <- ZIO.attempt(stream(handlerOutput))
        _ <- ZIO.attempt(writable.writeBytesTo(output))
      } yield ()
    }
}
