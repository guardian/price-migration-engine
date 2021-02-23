package pricemigrationengine.model

object Either {

  /** Converts a Seq of Eithers to either a Left
    * containing the first Left value of the Seq if any of the Seq has a Left value,
    * or else to a Right that contains the Seq of all the Right values in the original Seq.
    */
  // to keep the lambda package small we are not importing cats
  implicit final class TraverseOps[E, A](eithers: Seq[Either[E, A]]) {
    def sequence: Either[E, Seq[A]] =
      eithers
        .collectFirst { case Left(e) => e }
        .map(Left(_))
        .getOrElse(Right(eithers.collect { case Right(a) => a }))
  }
}
