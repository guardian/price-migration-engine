package pricemigrationengine.model

object EstimationHandlerHelper {

  def migrationCapRatio(cohortSpec: CohortSpec): Option[Double] = {
    // This is where we declare the optional capping of each migration
    MigrationType(cohortSpec) match {
      case Test1                  => None
      case SupporterPlus2024      => Some(1.27)
      case GuardianWeekly2025     => Some(1.2)
      case Newspaper2025P1        => Some(1.2)
      case HomeDelivery2025       => Some(1.2)
      case Newspaper2025P3        => Some(1.2)
      case ProductMigration2025N4 => None
      case Membership2025         => Some(1.3) // [1]
    }
    // [1] The 30% cap on Membership2025 is not going to be applied to
    // UK monthly subs moving from £7 to £10, which is a 42.86% increase
  }

  def commsPrice(cohortSpec: CohortSpec, oldPrice: BigDecimal, estimatedNewPriceUncapped: BigDecimal): BigDecimal = {
    PriceCap.cappedPrice(
      oldPrice,
      estimatedNewPriceUncapped,
      migrationCapRatio(cohortSpec: CohortSpec).map(ratio => BigDecimal(ratio))
    )
  }
}
