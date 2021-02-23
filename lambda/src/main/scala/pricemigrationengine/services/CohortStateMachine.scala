package pricemigrationengine.services

import com.amazonaws.services.stepfunctions.model.StartExecutionResult
import pricemigrationengine.model.{CohortSpec, CohortStateMachineFailure}
import zio.ZIO
import zio.clock.Clock

/** Kicks off the migration process for a particular cohort.
  *
  * The specification of the cohort is used as the input to the state machine.
  */
object CohortStateMachine {

  trait Service {
    def startExecution(spec: CohortSpec): ZIO[Clock, CohortStateMachineFailure, StartExecutionResult]
  }

  def startExecution(
      spec: CohortSpec
  ): ZIO[CohortStateMachine with Clock, CohortStateMachineFailure, StartExecutionResult] =
    ZIO.accessM(_.get.startExecution(spec))
}
