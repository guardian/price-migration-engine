package pricemigrationengine.services

import pricemigrationengine.model.{CohortSpec, CohortStateMachineFailure}
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse
import zio.ZIO
import zio.clock.Clock

/** Kicks off the migration process for a particular cohort.
  *
  * The specification of the cohort is used as the input to the state machine.
  */
object CohortStateMachine {

  trait Service {
    def startExecution(spec: CohortSpec): ZIO[Clock, CohortStateMachineFailure, StartExecutionResponse]
  }

  def startExecution(
      spec: CohortSpec
  ): ZIO[CohortStateMachine with Clock, CohortStateMachineFailure, StartExecutionResponse] =
    ZIO.accessM(_.get.startExecution(spec))
}
