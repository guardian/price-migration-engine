package pricemigrationengine.model

import pricemigrationengine.migrations.Membership2025Migration

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
    // [1] The 30% cap on Membership2025 is going to be applied
    // unless the increase came from the price grid.
  }

  def commsPrice(cohortSpec: CohortSpec, oldPrice: BigDecimal, estimatedNewPriceUncapped: BigDecimal): BigDecimal = {
    PriceCap.cappedPrice(
      oldPrice,
      estimatedNewPriceUncapped,
      migrationCapRatio(cohortSpec: CohortSpec).map(ratio => BigDecimal(ratio))
    )
  }

  /*
    Date: October 2025
    Author: Pascal

    As indicated in the mapping above, Membership2025 has special capping requirements.
    A price cap of 30% is going to be applied unless the price increase was mandated by the
    price grid ?

    Q: Why are we doing this ?
    A: Some old subs which didn't go through the previous price rise, if we rise them to
       the new price, will undergo too high of a rise, and for this we decided to introduce
       a cap, a 30% cap. (Which is 10 points higher than our standard 20%). With that said
       some price grid increase are higher than 30%, for instance the UK monthly is meant to
       go from 7 to 10, which is a 42% increase. To allows these intended rises to apply
       we have a special implementation of the comm price computation for Membership2025,
       by which we apply a cap unless the sub current (aka: old) price was the starting
       price of the price grid.

       For this we encoded the old prices of the price grid in the migration module
       as well as `subscriptionHasStandardOldPrice` to decide if the current price
       of the sub matches the starting price of the marketing price grid. When that is
       the case, we do not apply the price cap and just use the new price as intended.
   */

  def commsPriceForMembership2025(
      cohortSpec: CohortSpec,
      oldPrice: BigDecimal,
      estimatedNewPriceUncapped: BigDecimal,
      subscription: ZuoraSubscription,
      invoiceList: ZuoraInvoiceList
  ): BigDecimal = {

    // Note the use of `.get` here on an Option (subscriptionHasStandardOldPrice return
    // a Option[Boolean]). The engine will crash if we are in the very pathological
    // situation that this determination could not be done.

    if (Membership2025Migration.subscriptionHasStandardOldPrice(subscription, invoiceList).get) {
      estimatedNewPriceUncapped
    } else {
      PriceCap.cappedPrice(
        oldPrice,
        estimatedNewPriceUncapped,
        migrationCapRatio(cohortSpec: CohortSpec).map(ratio => BigDecimal(ratio)) // 30%
      )
    }
  }
}
