package pricemigrationengine.services

import pricemigrationengine.model.{CohortSpec, CohortStateMachineFailure}
import zio.ZIO

/**
  * Kicks off the migration process for a particular cohort.
  */
object CohortStateMachine {

  trait Service {
    def startExecution(spec: CohortSpec): ZIO[Any, CohortStateMachineFailure, Unit]
  }

  def startExecution(spec: CohortSpec): ZIO[CohortStateMachine, CohortStateMachineFailure, Unit] =
    ZIO.accessM(_.get.startExecution(spec))
}
