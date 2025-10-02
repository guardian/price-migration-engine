package pricemigrationengine.model

object EstimationHandlerHelper {

  def migrationCapRatio(cohortSpec: CohortSpec): Option[Double] = {
    // This is where we declare the optional capping of each subscription
    MigrationType(cohortSpec) match {
      case Test1                  => None
      case SupporterPlus2024      => None
      case GuardianWeekly2025     => Some(1.2)
      case Newspaper2025P1        => Some(1.2)
      case HomeDelivery2025       => Some(1.2)
      case Newspaper2025P3        => Some(1.2)
      case ProductMigration2025N4 => None
    }
  }

  def commsPrice(cohortSpec: CohortSpec, oldPrice: BigDecimal, estimatedNewPriceUncapped: BigDecimal): BigDecimal = {
    PriceCap.cappedPrice(
      oldPrice,
      estimatedNewPriceUncapped,
      migrationCapRatio(cohortSpec: CohortSpec).map(ratio => BigDecimal(ratio))
    )
  }
}
