package pricemigrationengine.services

import pricemigrationengine.handlers.Time
import pricemigrationengine.model._
import pricemigrationengine.services
import software.amazon.awssdk.services.sfn.model.{StartExecutionRequest, StartExecutionResponse}
import upickle.default.{ReadWriter, macroRW, write}
import zio.{Clock, IO, ZIO, ZLayer}

import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CohortStateMachineLive {

  private case class StateMachineInput(cohortSpec: CohortSpec)

  private implicit val rw: ReadWriter[StateMachineInput] = macroRW

  val impl: ZLayer[CohortStateMachineConfiguration with Logging with Clock, ConfigurationFailure, CohortStateMachine] =
    ZLayer.fromZIO {
      val stateMachine = AwsClient.sfn
      for {
        logging <- ZIO.service[Logging]
        config <- CohortStateMachineConfiguration.cohortStateMachineConfig
        clock <- ZIO.service[Clock]
      } yield new CohortStateMachine {
        override def startExecution(spec: CohortSpec): IO[CohortStateMachineFailure, StartExecutionResponse] =
          for {
            _ <- logging.info(s"Starting execution with input: ${spec.toString} ...")
            time <- Time.thisInstant.mapError(e => CohortStateMachineFailure(e.toString)).provideService(clock)
            timeStr <- ZIO
              .attempt(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm").withZone(ZoneId.systemDefault).format(time))
              .mapError(e => CohortStateMachineFailure(e.toString))
            result <- ZIO
              .attemptBlocking(
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
