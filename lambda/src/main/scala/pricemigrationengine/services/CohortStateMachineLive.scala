package pricemigrationengine.services

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.amazonaws.regions.Regions.EU_WEST_1
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder
import com.amazonaws.services.stepfunctions.model.{StartExecutionRequest, StartExecutionResult}
import pricemigrationengine.handlers.Time
import pricemigrationengine.model._
import upickle.default.{ReadWriter, macroRW, write}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{ZIO, ZLayer}

object CohortStateMachineLive {

  private case class StateMachineInput(cohortSpec: CohortSpec)
  private implicit val rw: ReadWriter[StateMachineInput] = macroRW

  val impl
      : ZLayer[CohortStateMachineConfiguration with Logging with Blocking, ConfigurationFailure, CohortStateMachine] = {

    val stateMachine = AWSStepFunctionsClientBuilder.standard.withRegion(EU_WEST_1).build

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

          def startExecution(spec: CohortSpec): ZIO[Clock, CohortStateMachineFailure, StartExecutionResult] =
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
                      new StartExecutionRequest()
                        .withStateMachineArn(config.stateMachineArn)
                        .withName(s"${spec.normalisedCohortName}-$timeStr")
                        .withInput(write(StateMachineInput(spec)))
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
