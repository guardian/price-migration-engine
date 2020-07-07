package pricemigrationengine.services

import java.time.LocalDate

import com.amazonaws.regions.Regions.EU_WEST_1
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder
import com.amazonaws.services.stepfunctions.model.{StartExecutionRequest, StartExecutionResult}
import pricemigrationengine.model._
import upickle.default.write
import zio.blocking.Blocking
import zio.{IO, ZLayer}

object CohortStateMachineLive {

  val impl
    : ZLayer[CohortStateMachineConfiguration with Logging with Blocking, ConfigurationFailure, CohortStateMachine] = {

    val stateMachine = AWSStepFunctionsClientBuilder.standard.withRegion(EU_WEST_1).build

    ZLayer.fromServicesM[CohortStateMachineConfiguration.Service,
                         Logging.Service,
                         Blocking.Service,
                         Any,
                         ConfigurationFailure,
                         CohortStateMachine.Service] { (configuration, logging, blocking) =>
      configuration.config map { config =>
        new CohortStateMachine.Service {
          def startExecution(date: LocalDate)(spec: CohortSpec): IO[CohortStateMachineFailure, StartExecutionResult] =
            blocking
              .effectBlocking(
                stateMachine.startExecution(
                  new StartExecutionRequest()
                    .withStateMachineArn(config.stateMachineArn)
                    .withName(s"${spec.cohortName}-$date")
                    .withInput(write(spec))))
              .mapError(e => CohortStateMachineFailure(s"Failed to start execution: $e"))
              .tap(result => logging.info(s"Started execution: $result"))
        }
      }
    }
  }
}
