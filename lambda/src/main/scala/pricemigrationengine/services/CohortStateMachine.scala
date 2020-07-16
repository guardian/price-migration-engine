package pricemigrationengine.services

import java.time.LocalDate

import com.amazonaws.services.stepfunctions.model.StartExecutionResult
import pricemigrationengine.model.{CohortSpec, CohortStateMachineFailure}
import zio.ZIO

/**
  * Kicks off the migration process for a particular cohort.
  *
  * The specification of the cohort is used as the input to the state machine.
  */
object CohortStateMachine {

  trait Service {
    def startExecution(date: LocalDate)(spec: CohortSpec): ZIO[Any, CohortStateMachineFailure, StartExecutionResult]
  }

  def startExecution(date: LocalDate)(
      spec: CohortSpec): ZIO[CohortStateMachine, CohortStateMachineFailure, StartExecutionResult] =
    ZIO.accessM(_.get.startExecution(date)(spec))
}
