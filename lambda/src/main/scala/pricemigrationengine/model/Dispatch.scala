package pricemigrationengine.model

import scala.util.hashing.MurmurHash3

object Dispatch {

  /*
    The Displach functionality was introduced to help with SupporterPlus2026
    https://github.com/guardian/price-migration-engine/pull/1500

    ... and the `evaluate` function is the core of it. It takes a string (essentially a subscription number)
    and an integer between 1 and 5 (we have 5 names for SupporterPlus2026) and return a boolean.

    The requirements are
    1. For any string, there must be an integer n such that evaluate(string, n) is true
    2. For any string, there is a unique integer n such evaluate(string, n) is true

    This implies that the space of strings is partitioned into 5 subsets
   */
  def evaluate(s: String, n: Int): Boolean = {
    require(n >= 1 && n <= 5, "n must be between 1 and 5")
    val bucket = Math.abs(MurmurHash3.stringHash(s) % 5)
    bucket == n - 1
  }

  /*
    This function returns whether the cohort item "belongs" to the migration type
    It defaults to true for non SupporterPlus2026 migrations, but induces a partition
    in 5 subsets for SupporterPlus2026
   */
  def belongs(cohortSpec: CohortSpec, cohortItem: CohortItem): Boolean = {
    MigrationType(cohortSpec) match {
      case Test1                  => true
      case GuardianWeekly2025     => true
      case Newspaper2025P1        => true
      case Newspaper2025P3        => true
      case ProductMigration2025N4 => true
      case Membership2025         => true
      case DigiSubs2025           => true
      case SupporterPlus2026      => evaluate(cohortItem.subscriptionName, 1)
      case SupporterPlus2026N2    => evaluate(cohortItem.subscriptionName, 2)
      case SupporterPlus2026N3    => evaluate(cohortItem.subscriptionName, 3)
      case SupporterPlus2026N4    => evaluate(cohortItem.subscriptionName, 4)
      case SupporterPlus2026N5    => evaluate(cohortItem.subscriptionName, 5)
    }
  }

}
