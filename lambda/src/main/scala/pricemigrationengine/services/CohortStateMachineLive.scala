package pricemigrationengine.services

import pricemigrationengine.handlers.Time
import pricemigrationengine.model._
import software.amazon.awssdk.services.sfn.model.{StartExecutionRequest, StartExecutionResponse}
import upickle.default.{ReadWriter, macroRW, write}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{ZIO, ZLayer}

import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CohortStateMachineLive {

  private case class StateMachineInput(cohortSpec: CohortSpec)

  private implicit val rw: ReadWriter[StateMachineInput] = macroRW

  val impl
      : ZLayer[CohortStateMachineConfiguration with Logging with Blocking, ConfigurationFailure, CohortStateMachine] = {

    val stateMachine = AwsClient.sfn

    ZLayer.fromServicesM[
      CohortStateMachineConfiguration.Service,
      Logging.Service,
      Blocking.Service,
      Any,
      ConfigurationFailure,
      CohortStateMachine.Service
    ] { (configuration, logging, blocking) =>
      configuration.config map { config =>
        //noinspection ConvertExpressionToSAM
        new CohortStateMachine.Service {

          def startExecution(spec: CohortSpec): ZIO[Clock, CohortStateMachineFailure, StartExecutionResponse] =
            for {
              _ <- logging.info(s"Starting execution with input: ${spec.toString} ...")
              time <- Time.thisInstant.mapError(e => CohortStateMachineFailure(e.toString))
              timeStr <-
                ZIO
                  .effect(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm").withZone(ZoneId.systemDefault).format(time))
                  .mapError(e => CohortStateMachineFailure(e.toString))
              result <-
                blocking
                  .effectBlocking(
                    stateMachine.startExecution(
                      StartExecutionRequest.builder
                        .stateMachineArn(config.stateMachineArn)
                        .name(s"${spec.normalisedCohortName}-$timeStr")
                        .input(write(StateMachineInput(spec)))
                        .build()
                    )
                  )
                  .mapError(e => CohortStateMachineFailure(s"Failed to start execution: $e"))
                  .tap(result => logging.info(s"Started execution: $result"))
            } yield result
        }
      }
    }
  }
}
