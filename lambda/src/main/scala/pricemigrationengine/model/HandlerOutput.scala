package pricemigrationengine.model

import upickle.default.{ReadWriter, macroRW}

/**
  * Output of a handler.
  *
  * This is useful in the context of a state machine because it enables us to change the behaviour of the machine.
  */
case class HandlerOutput(
    /**
      * Specification that was the input to the handler.
      */
    cohortSpec: CohortSpec,
    /**
      * True iff the process completed in the time available to the handler.
      */
    isComplete: Boolean
)

object HandlerOutput {
  implicit val rwSubscription: ReadWriter[HandlerOutput] = macroRW
}
