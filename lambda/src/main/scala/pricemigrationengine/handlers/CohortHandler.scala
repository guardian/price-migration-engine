package pricemigrationengine.handlers

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import pricemigrationengine.model._
import pricemigrationengine.services._
import pricemigrationengine.model.Runner.unsafeRun
import ujson.Readable
import upickle.default.{read, stream}
import zio.{Runtime, ZIO, ZIOAppArgs, ZIOAppDefault}

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
  def handle(input: CohortSpec): ZIO[Logging, Failure, HandlerOutput]

  private def goConsole(input: Readable): ZIO[Logging, Failure, HandlerOutput] =
    (for {
      cohortSpec <- validSpec(input)
      output <- handle(cohortSpec).provideLayer(ConsoleLogging.impl(cohortSpec.cohortName))
    } yield output)
      .tapBoth(
        failure => Logging.error(failure.reason),
        output => Logging.info(s"Output: $output")
      )

  private def goLambda(input: Readable, context: Context): ZIO[Logging, Failure, HandlerOutput] =
    (for {
      cohortSpec <- validSpec(input)
      output <- handle(cohortSpec).provideLayer(LambdaLogging.impl(context, cohortSpec.cohortName))
    } yield output)
      .tapBoth(
        failure => Logging.error(failure.reason),
        output => Logging.info(s"Output: $output")
      )

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

  /** First, and only expected, program argument is a Json string representing the input to the lambda.
    * Eg:
    * {
    *     "cohortSpec":{
    *         "cohortName":"M2023",
    *         "brazeName":"SV_MB_M2023",
    *         "earliestPriceMigrationStartDate":"2023-01-02"
    *     }
    * }
    */
  override final def run: ZIO[ZIOAppArgs, Any, Any] =
    (for {
      args <- ZIOAppArgs.getArgs
      input <- ZIO
        .fromOption(args.headOption)
        .orElseFail(InputFailure("No input"))
        .tapError(e => Logging.error(e.toString))
      _ <- goConsole(input)
    } yield ())
      .provideSomeLayer[ZIOAppArgs](ConsoleLogging.impl("ParsingEnvInput"))

  override final def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit =
    unsafeRun(Runtime.default)(
      for {
        handlerOutput <- goLambda(input, context).provideLayer(LambdaLogging.impl(context, "ParsingCohortInfo"))
        writable <- ZIO.attempt(stream(handlerOutput))
        _ <- ZIO.attempt(writable.writeBytesTo(output))
      } yield ()
    )
}
