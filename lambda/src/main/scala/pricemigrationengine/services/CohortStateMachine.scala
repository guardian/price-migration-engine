package pricemigrationengine.services

import pricemigrationengine.model.{CohortSpec, CohortStateMachineFailure}
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse
import zio.{IO, ZIO}

/** Kicks off the migration process for a particular cohort.
  *
  * The specification of the cohort is used as the input to the state machine.
  */
object CohortStateMachine {

  trait Service {
    def startExecution(spec: CohortSpec): IO[CohortStateMachineFailure, StartExecutionResponse]
  }

  def startExecution(
      spec: CohortSpec
  ): ZIO[CohortStateMachine, CohortStateMachineFailure, StartExecutionResponse] =
    ZIO.serviceWithZIO(_.startExecution(spec))
}
