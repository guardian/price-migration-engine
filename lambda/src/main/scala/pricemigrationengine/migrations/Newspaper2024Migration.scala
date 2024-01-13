package pricemigrationengine.migrations
import pricemigrationengine.model._

import java.time.LocalDate

object Newspaper2024MigrationStaticData {

  val maxLeadTime = 40
  val minLeadTime = 35

  /*
    Correspondence between product names in Salesforce versus Zuora

    -----------------------------------------------------------------
    Product name in the Salesforce export | Product name in Zuora
    -----------------------------------------------------------------
    Newspaper - Home Delivery             | Newspaper Delivery
    Newspaper - Subscription Card         | Newspaper Digital Voucher
    Newspaper - Voucher Book              | Newspaper Voucher
    -----------------------------------------------------------------
   */

  /*
    The pricing system, explained (Part 1):

    The pricing for the Newspaper2024 migration was given as a price matrix, which was essentially a map
    (product, billingPeriod, ratePlanName) -> price

    For instance, for product "Newspaper - Home Delivery" (aka "Newspaper Delivery"), there are two possible frequencies:
    "Month" and "Quarter". Considering "Month", we then have 10 possible rate plans: "Everyday". "Sixday", "Weekend",
    "Saturday", "Sunday", and their "+" (digipack) equivalent: "Everyday+". "Sixday+", "Weekend+",
    "Saturday+", "Sunday+". According to the matrix ("Newspaper - Home Delivery", "Month", "Everyday") evaluates to £78.99

    The entire price matrix is implemented in the variables

        - newspaperHomeDeliveryMonthlyPrices
        - newspaperHomeDeliveryQuarterlyPrices

        - newspaperSubscriptionCardMonthlyPrices
        - newspaperSubscriptionCardQuarterlyPrices
        - newspaperSubscriptionCardSemiAnnualPrices
        - newspaperSubscriptionCardAnnualPrices

        - newspaperVoucherBookMonthlyPrices
        - newspaperVoucherBookQuarterlyPrices
        - newspaperVoucherBookSemiAnnualPrices
        - newspaperVoucherBookAnnualPrices

    And the price given by the function
        priceLookup(product: String, billingPeriod: BillingPeriod, ratePlanName: String): Option[BigDecimal]

    Those prices are mostly used during the estimation process andhelp define the "new price" of a subscription.

    ps: If you want a copy of the price matrix, ask Pascal or somebody from marketing.

   */

  val newspaperHomeDeliveryMonthlyPrices: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(78.99),
    "Sixday" -> BigDecimal(68.99),
    "Weekend" -> BigDecimal(31.99),
    "Saturday" -> BigDecimal(19.99),
    "Sunday" -> BigDecimal(19.99),
    "Everyday+" -> BigDecimal(80.99),
    "Sixday+" -> BigDecimal(70.99),
    "Weekend+" -> BigDecimal(40.99),
    "Saturday+" -> BigDecimal(30.99),
    "Sunday+" -> BigDecimal(30.99),
  )

  val newspaperHomeDeliveryQuarterlyPrices: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(236.97),
    "Sixday" -> BigDecimal(206.97),
    "Weekend" -> BigDecimal(95.97),
    "Saturday" -> BigDecimal(59.97),
    "Sunday" -> BigDecimal(59.97),
  )

  val newspaperSubscriptionCardMonthlyPrices: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(64.99),
    "Sixday" -> BigDecimal(56.99),
    "Weekend" -> BigDecimal(25.99),
    "Saturday" -> BigDecimal(14.99),
    "Sunday" -> BigDecimal(14.99),
    "Everyday+" -> BigDecimal(66.99),
    "Sixday+" -> BigDecimal(58.99),
    "Weekend+" -> BigDecimal(34.99),
    "Saturday+" -> BigDecimal(25.99),
    "Sunday+" -> BigDecimal(25.99),
  )

  val newspaperSubscriptionCardQuarterlyPrices: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(194.97),
    "Sixday" -> BigDecimal(170.97),
    "Weekend" -> BigDecimal(77.97),
    "Everyday+" -> BigDecimal(200.97),
    "Sixday+" -> BigDecimal(176.97),
  )

  val newspaperSubscriptionCardSemiAnnualPrices: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(389.94),
    "Sixday" -> BigDecimal(341.94),
    "Everyday+" -> BigDecimal(401.94),
  )

  val newspaperSubscriptionCardAnnualPrices: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(779.88),
    "Sixday" -> BigDecimal(683.88),
    "Weekend" -> BigDecimal(311.88),
  )

  val newspaperVoucherBookMonthlyPrices: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(64.99),
    "Sixday" -> BigDecimal(56.99),
    "Weekend" -> BigDecimal(25.99),
    "Saturday" -> BigDecimal(14.99),
    "Sunday" -> BigDecimal(14.99),
    "Everyday+" -> BigDecimal(66.99),
    "Sixday+" -> BigDecimal(58.99),
    "Weekend+" -> BigDecimal(34.99),
    "Saturday+" -> BigDecimal(25.99),
    "Sunday+" -> BigDecimal(25.99),
  )

  val newspaperVoucherBookQuarterlyPrices: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(194.97),
    "Sixday" -> BigDecimal(170.97),
    "Weekend" -> BigDecimal(77.97),
    "Everyday+" -> BigDecimal(200.97),
    "Sixday+" -> BigDecimal(176.97),
    "Weekend+" -> BigDecimal(104.97),
    "Sunday+" -> BigDecimal(77.97),
  )

  val newspaperVoucherBookSemiAnnualPrices: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(389.94),
    "Sixday" -> BigDecimal(341.94),
    "Weekend" -> BigDecimal(155.94),
    "Everyday+" -> BigDecimal(401.94),
    "Sixday+" -> BigDecimal(353.94),
    "Weekend+" -> BigDecimal(209.94),
    "Sunday+" -> BigDecimal(155.94),
  )

  val newspaperVoucherBookAnnualPrices: Map[String, BigDecimal] = Map(
    "Everyday" -> BigDecimal(779.88),
    "Sixday" -> BigDecimal(683.88),
    "Weekend" -> BigDecimal(311.88),
    "Everyday+" -> BigDecimal(803.88),
    "Sixday+" -> BigDecimal(707.88),
    "Weekend+" -> BigDecimal(419.88),
  )

  def priceLookup(product: String, billingPeriod: BillingPeriod, ratePlanName: String): Option[BigDecimal] = {
    val empty: Map[String, BigDecimal] = Map()
    val priceMap = (product, billingPeriod) match {
      case ("Newspaper Delivery", Monthly)           => newspaperHomeDeliveryMonthlyPrices
      case ("Newspaper Delivery", Quarterly)         => newspaperHomeDeliveryQuarterlyPrices
      case ("Newspaper Digital Voucher", Monthly)    => newspaperSubscriptionCardMonthlyPrices
      case ("Newspaper Digital Voucher", Quarterly)  => newspaperSubscriptionCardQuarterlyPrices
      case ("Newspaper Digital Voucher", SemiAnnual) => newspaperSubscriptionCardSemiAnnualPrices
      case ("Newspaper Digital Voucher", Annual)     => newspaperSubscriptionCardAnnualPrices
      case ("Newspaper Voucher", Monthly)            => newspaperVoucherBookMonthlyPrices
      case ("Newspaper Voucher", Quarterly)          => newspaperVoucherBookQuarterlyPrices
      case ("Newspaper Voucher", SemiAnnual)         => newspaperVoucherBookSemiAnnualPrices
      case ("Newspaper Voucher", Annual)             => newspaperVoucherBookAnnualPrices
      case _                                         => empty
    }
    priceMap.get(ratePlanName)
  }

  /*
    The pricing system, explained (Part 2):

    Things would be quite simple if the price from the pricing matrix was the only charge on the rate plan, but that is
    not the case. For instance in the case of `("Newspaper - Home Delivery", "Month", "Everyday") -> £78.99`
    the price actually breakdown in the following charges

        monday    -> 10.24
        tuesday   -> 10.24
        wednesday -> 10.24
        thursday  -> 10.24
        friday    -> 10.24
        saturday  -> 13.89
        sunday    -> 13.90

    Note that the value per day is not constant (the value for saturday and sunday is higher), but the important things
    is that the sum 10.24 + 10.24 + 10.24 + 10.24 + 10.24 + 13.89 + 13.90 is what we had in the price matrix: 78.99

    The digipack variant is also spread down per day but in addition has a value for digipack. Therefore
    `("Newspaper - Home Delivery", "Month", "Everyday+")` breaks down as the following charges

        monday    -> 10.24
        tuesday   -> 10.24
        wednesday -> 10.24
        thursday  -> 10.24
        friday    -> 10.24
        saturday  -> 13.89
        sunday    -> 13.90
        digiPack  -> 2.00

    which leads to the 80.99 we have in the price matrix.

    To encapsulate this information, we use `case class PriceDistribution`
   */

  case class PriceDistribution(
      monday: Option[BigDecimal],
      tuesday: Option[BigDecimal],
      wednesday: Option[BigDecimal],
      thursday: Option[BigDecimal],
      friday: Option[BigDecimal],
      saturday: Option[BigDecimal],
      sunday: Option[BigDecimal],
      digitalPack: Option[BigDecimal]
  )

  /*
    The pricing system, explained (Part 3):

    In Part 1 we have seen the price matrix and how it's encoded and queried and in Part 2 we have seen
    the PriceDistribution, an in particular the price distribution for `("Newspaper - Home Delivery", "Month", "Everyday")`

    That price distribution is found in Zuora. We have a product catalogue definition for `("Newspaper - Home Delivery", "Month", "Everyday")`,
    and in fact we have a catalogue definition for any `("Newspaper - Home Delivery", "Month", *)`, but we do not have
    a product catalogue entry of `("Newspaper - Home Delivery", "Quarter", *)`

    The prices for `("Newspaper - Home Delivery", "Quarter", "Everyday")` are 3 times the prices of the month(ly),
    therefore:
        monday    -> 30.72
        tuesday   -> 30.72
        wednesday -> 30.72
        thursday  -> 30.72
        friday    -> 30.72
        saturday  -> 41.67
        sunday    -> 41.70

        which sums to 30.72 + 30.72 + 30.72 + 30.72 + 30.72 + 41.67 + 41.70 = 236.97, which is the price we find in
        the price matrix for `("Newspaper - Home Delivery", "Quarter", "Everyday")`.

    Unlike te case of newspaperHomeDeliveryMonthlyPriceDistributions, aka `("Newspaper - Home Delivery", "Month", "Everyday")`,
    where we had to look up the rate plan charges in the Zuora product catalogue and hardcode the values in a PriceDistribution,
    in the case of `("Newspaper - Home Delivery", "Quarter", "Everyday")`, the price distribution has not been hard coded
    (mostly to reduce the risk of human error and typos) and is derived from the month version simply by multiplying it by
    3. For this we have the function `priceDistributionMultiplier(pd: PriceDistribution, multiplier: Int)`

    The general pattern is therefore to have hard coded price distributions for the monthly rate plans, and to use the
    multiplier to derive the quarterly, semi-annual and annual price distributions.
   */

  def priceDistributionMultiplier(pd: PriceDistribution, multiplier: Int): PriceDistribution = {
    def mult(bd: Option[BigDecimal]): Option[BigDecimal] = bd.map(bd => bd * BigDecimal(multiplier))

    PriceDistribution(
      monday = mult(pd.monday),
      tuesday = mult(pd.tuesday),
      wednesday = mult(pd.wednesday),
      thursday = mult(pd.thursday),
      friday = mult(pd.friday),
      saturday = mult(pd.saturday),
      sunday = mult(pd.sunday),
      digitalPack = mult(pd.digitalPack)
    )
  }

  val newspaperHomeDeliveryMonthlyPriceDistributions: Map[String, PriceDistribution] = Map(
    "Everyday" -> PriceDistribution(
      monday = Some(BigDecimal(10.24)),
      tuesday = Some(BigDecimal(10.24)),
      wednesday = Some(BigDecimal(10.24)),
      thursday = Some(BigDecimal(10.24)),
      friday = Some(BigDecimal(10.24)),
      saturday = Some(BigDecimal(13.89)),
      sunday = Some(BigDecimal(13.90)),
      digitalPack = None,
    ),
    "Sixday" -> PriceDistribution(
      monday = Some(BigDecimal(10.85)),
      tuesday = Some(BigDecimal(10.85)),
      wednesday = Some(BigDecimal(10.85)),
      thursday = Some(BigDecimal(10.85)),
      friday = Some(BigDecimal(10.85)),
      saturday = Some(BigDecimal(14.74)),
      sunday = None,
      digitalPack = None,
    ),
    "Weekend" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(15.99)),
      Some(BigDecimal(16.00)),
      None
    ),
    "Saturday" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(19.99)),
      None,
    ),
    "Sunday" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(19.99)),
      None,
    ),
    "Everyday+" -> PriceDistribution(
      monday = Some(BigDecimal(10.24)),
      tuesday = Some(BigDecimal(10.24)),
      wednesday = Some(BigDecimal(10.24)),
      thursday = Some(BigDecimal(10.24)),
      friday = Some(BigDecimal(10.24)),
      saturday = Some(BigDecimal(13.89)),
      sunday = Some(BigDecimal(13.90)),
      digitalPack = Some(BigDecimal(2.00)),
    ),
    "Sixday+" -> PriceDistribution(
      monday = Some(BigDecimal(10.85)),
      tuesday = Some(BigDecimal(10.85)),
      wednesday = Some(BigDecimal(10.85)),
      thursday = Some(BigDecimal(10.85)),
      friday = Some(BigDecimal(10.85)),
      saturday = Some(BigDecimal(14.74)),
      sunday = None,
      digitalPack = Some(BigDecimal(2.00)),
    ),
    "Weekend+" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(15.99)),
      Some(BigDecimal(16.00)),
      digitalPack = Some(BigDecimal(9.00)),
    ),
    "Saturday+" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(19.99)),
      digitalPack = Some(BigDecimal(11.00)),
    ),
    "Sunday+" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(19.99)),
      digitalPack = Some(BigDecimal(11.00)),
    ),
  )

  val newspaperHomeDeliveryQuarterlyPriceDistributions: Map[String, PriceDistribution] = Map(
    "Everyday" -> priceDistributionMultiplier(newspaperHomeDeliveryMonthlyPriceDistributions("Everyday"), 3),
    "Sixday" -> priceDistributionMultiplier(newspaperHomeDeliveryMonthlyPriceDistributions("Sixday"), 3),
    "Weekend" -> priceDistributionMultiplier(newspaperHomeDeliveryMonthlyPriceDistributions("Weekend"), 3),
    "Saturday" -> priceDistributionMultiplier(newspaperHomeDeliveryMonthlyPriceDistributions("Saturday"), 3),
    "Sunday" -> priceDistributionMultiplier(newspaperHomeDeliveryMonthlyPriceDistributions("Sunday"), 3),
  )

  val newspaperSubscriptionCardMonthlyPriceDistributions: Map[String, PriceDistribution] = Map(
    "Everyday" -> PriceDistribution(
      monday = Some(BigDecimal(8.42)),
      tuesday = Some(BigDecimal(8.42)),
      wednesday = Some(BigDecimal(8.42)),
      thursday = Some(BigDecimal(8.42)),
      friday = Some(BigDecimal(8.42)),
      saturday = Some(BigDecimal(11.44)),
      sunday = Some(BigDecimal(11.45)),
      digitalPack = None,
    ),
    "Sixday" -> PriceDistribution(
      monday = Some(BigDecimal(8.96)),
      tuesday = Some(BigDecimal(8.96)),
      wednesday = Some(BigDecimal(8.96)),
      thursday = Some(BigDecimal(8.96)),
      friday = Some(BigDecimal(8.96)),
      saturday = Some(BigDecimal(12.19)),
      sunday = None,
      digitalPack = None,
    ),
    "Weekend" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(12.99)),
      Some(BigDecimal(13.00)),
      None
    ),
    "Saturday" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(14.99)),
      None,
    ),
    "Sunday" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(14.99)),
      digitalPack = None,
    ),
    "Everyday+" -> PriceDistribution(
      monday = Some(BigDecimal(8.42)),
      tuesday = Some(BigDecimal(8.42)),
      wednesday = Some(BigDecimal(8.42)),
      thursday = Some(BigDecimal(8.42)),
      friday = Some(BigDecimal(8.42)),
      saturday = Some(BigDecimal(11.44)),
      sunday = Some(BigDecimal(11.45)),
      digitalPack = Some(BigDecimal(2.00)),
    ),
    "Sixday+" -> PriceDistribution(
      monday = Some(BigDecimal(8.96)),
      tuesday = Some(BigDecimal(8.96)),
      wednesday = Some(BigDecimal(8.96)),
      thursday = Some(BigDecimal(8.96)),
      friday = Some(BigDecimal(8.96)),
      saturday = Some(BigDecimal(12.19)),
      sunday = None,
      digitalPack = Some(BigDecimal(2.00)),
    ),
    "Weekend+" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(12.99)),
      Some(BigDecimal(13.00)),
      digitalPack = Some(BigDecimal(9.00)),
    ),
    "Saturday+" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(14.99)),
      digitalPack = Some(BigDecimal(11.00)),
    ),
    "Sunday+" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(14.99)),
      digitalPack = Some(BigDecimal(11.00)),
    ),
  )

  val newspaperSubscriptionCardQuarterlyPriceDistributions: Map[String, PriceDistribution] = Map(
    "Everyday" -> priceDistributionMultiplier(newspaperSubscriptionCardMonthlyPriceDistributions("Everyday"), 3),
    "Sixday" -> priceDistributionMultiplier(newspaperSubscriptionCardMonthlyPriceDistributions("Sixday"), 3),
    "Weekend" -> priceDistributionMultiplier(newspaperSubscriptionCardMonthlyPriceDistributions("Weekend"), 3),
    "Everyday+" -> priceDistributionMultiplier(newspaperSubscriptionCardMonthlyPriceDistributions("Everyday+"), 3),
    "Sixday+" -> priceDistributionMultiplier(newspaperSubscriptionCardMonthlyPriceDistributions("Sixday+"), 3),
  )

  val newspaperSubscriptionCardSemiAnnualPriceDistributions: Map[String, PriceDistribution] = Map(
    "Everyday" -> priceDistributionMultiplier(newspaperSubscriptionCardMonthlyPriceDistributions("Everyday"), 6),
    "Sixday" -> priceDistributionMultiplier(newspaperSubscriptionCardMonthlyPriceDistributions("Sixday"), 6),
    "Everyday+" -> priceDistributionMultiplier(newspaperSubscriptionCardMonthlyPriceDistributions("Everyday+"), 6),
  )

  val newspaperSubscriptionCardAnnualPriceDistributions: Map[String, PriceDistribution] = Map(
    "Everyday" -> priceDistributionMultiplier(newspaperSubscriptionCardMonthlyPriceDistributions("Everyday"), 12),
    "Sixday" -> priceDistributionMultiplier(newspaperSubscriptionCardMonthlyPriceDistributions("Sixday"), 12),
    "Weekend" -> priceDistributionMultiplier(newspaperSubscriptionCardMonthlyPriceDistributions("Weekend"), 12),
  )

  val newspaperVoucherBookMonthlyPriceDistibutions: Map[String, PriceDistribution] = Map(
    "Everyday" -> PriceDistribution(
      monday = Some(BigDecimal(8.42)),
      tuesday = Some(BigDecimal(8.42)),
      wednesday = Some(BigDecimal(8.42)),
      thursday = Some(BigDecimal(8.42)),
      friday = Some(BigDecimal(8.42)),
      saturday = Some(BigDecimal(11.44)),
      sunday = Some(BigDecimal(11.45)),
      digitalPack = None,
    ),
    "Sixday" -> PriceDistribution(
      monday = Some(BigDecimal(8.96)),
      tuesday = Some(BigDecimal(8.96)),
      wednesday = Some(BigDecimal(8.96)),
      thursday = Some(BigDecimal(8.96)),
      friday = Some(BigDecimal(8.96)),
      saturday = Some(BigDecimal(12.19)),
      sunday = None,
      digitalPack = None,
    ),
    "Weekend" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(12.99)),
      Some(BigDecimal(13.00)),
      None
    ),
    "Saturday" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(14.99)),
      None,
    ),
    "Sunday" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(14.99)),
      digitalPack = None,
    ),
    "Everyday+" -> PriceDistribution(
      monday = Some(BigDecimal(8.42)),
      tuesday = Some(BigDecimal(8.42)),
      wednesday = Some(BigDecimal(8.42)),
      thursday = Some(BigDecimal(8.42)),
      friday = Some(BigDecimal(8.42)),
      saturday = Some(BigDecimal(11.44)),
      sunday = Some(BigDecimal(11.45)),
      digitalPack = Some(BigDecimal(2.00)),
    ),
    "Sixday+" -> PriceDistribution(
      monday = Some(BigDecimal(8.96)),
      tuesday = Some(BigDecimal(8.96)),
      wednesday = Some(BigDecimal(8.96)),
      thursday = Some(BigDecimal(8.96)),
      friday = Some(BigDecimal(8.96)),
      saturday = Some(BigDecimal(12.19)),
      sunday = None,
      digitalPack = Some(BigDecimal(2.00)),
    ),
    "Weekend+" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(12.99)),
      Some(BigDecimal(13.00)),
      digitalPack = Some(BigDecimal(9.00)),
    ),
    "Saturday+" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(14.99)),
      digitalPack = Some(BigDecimal(11.00)),
    ),
    "Sunday+" -> PriceDistribution(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(BigDecimal(14.99)),
      digitalPack = Some(BigDecimal(11.00)),
    ),
  )

  val newspaperVoucherBookQuarterlyPriceDistibutions: Map[String, PriceDistribution] = Map(
    "Everyday" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Everyday"), 3),
    "Sixday" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Sixday"), 3),
    "Weekend" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Weekend"), 3),
    "Everyday+" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Everyday+"), 3),
    "Sixday+" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Sixday+"), 3),
    "Weekend+" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Weekend+"), 3),
    "Sunday+" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Sunday+"), 3),
  )

  val newspaperVoucherBookSemiAnnualPriceDistributions: Map[String, PriceDistribution] = Map(
    "Everyday" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Everyday"), 6),
    "Sixday" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Sixday"), 6),
    "Weekend" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Weekend"), 6),
    "Everyday+" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Everyday+"), 6),
    "Sixday+" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Sixday+"), 6),
    "Weekend+" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Weekend+"), 6),
    "Sunday+" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Sunday+"), 6),
  )

  val newspaperVoucherBookAnnualPriceDistributions: Map[String, PriceDistribution] = Map(
    "Everyday" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Everyday"), 12),
    "Sixday" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Sixday"), 12),
    "Weekend" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Weekend"), 12),
    "Everyday+" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Everyday+"), 12),
    "Sixday+" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Sixday+"), 12),
    "Weekend+" -> priceDistributionMultiplier(newspaperVoucherBookMonthlyPriceDistibutions("Weekend+"), 12),
  )

  /*
  The pricing system, explained (Part 4):

  The last pieces of the pricing puzzle are

  1. A function that perform price distribution lookups

  2. A function that turns a PriceDistribution into a price by summing the non None components. It's used in
  tests to ensure consistency between the price matrix and the price distributions
   */

  def priceDistributionLookup(
      product: String,
      billingPeriod: BillingPeriod,
      ratePlanName: String
  ): Option[PriceDistribution] = {
    val empty: Map[String, PriceDistribution] = Map()
    val priceMap = (product, billingPeriod) match {
      case ("Newspaper Delivery", Monthly)           => newspaperHomeDeliveryMonthlyPriceDistributions
      case ("Newspaper Delivery", Quarterly)         => newspaperHomeDeliveryQuarterlyPriceDistributions
      case ("Newspaper Digital Voucher", Monthly)    => newspaperSubscriptionCardMonthlyPriceDistributions
      case ("Newspaper Digital Voucher", Quarterly)  => newspaperSubscriptionCardQuarterlyPriceDistributions
      case ("Newspaper Digital Voucher", SemiAnnual) => newspaperSubscriptionCardSemiAnnualPriceDistributions
      case ("Newspaper Digital Voucher", Annual)     => newspaperSubscriptionCardAnnualPriceDistributions
      case ("Newspaper Voucher", Monthly)            => newspaperVoucherBookMonthlyPriceDistibutions
      case ("Newspaper Voucher", Quarterly)          => newspaperVoucherBookQuarterlyPriceDistibutions
      case ("Newspaper Voucher", SemiAnnual)         => newspaperVoucherBookSemiAnnualPriceDistributions
      case ("Newspaper Voucher", Annual)             => newspaperVoucherBookAnnualPriceDistributions
      case _                                         => empty
    }
    priceMap.get(ratePlanName)
  }

  def priceDistributionToPrice(distribution: PriceDistribution): BigDecimal = {
    List(
      distribution.monday.getOrElse(BigDecimal(0)),
      distribution.tuesday.getOrElse(BigDecimal(0)),
      distribution.wednesday.getOrElse(BigDecimal(0)),
      distribution.thursday.getOrElse(BigDecimal(0)),
      distribution.friday.getOrElse(BigDecimal(0)),
      distribution.saturday.getOrElse(BigDecimal(0)),
      distribution.sunday.getOrElse(BigDecimal(0)),
      distribution.digitalPack.getOrElse(BigDecimal(0))
    ).foldLeft(BigDecimal(0))((sum, item) => sum + item)
  }

}

object Newspaper2024Migration {

  // --------------------------------------------------------------------------------------------

  // RatePlanDetails was introduced to help properly test the data gathering to build the PriceData
  // It turned out to be particularly useful for testing that the logic was correct
  // It is currently limited to Newspaper2024Migration, but could be generalised to other (future) migrations
  case class RatePlanDetails(
      ratePlan: ZuoraRatePlan,
      ratePlanName: String,
      billingPeriod: BillingPeriod,
      currency: String,
      currentPrice: BigDecimal
  )

  // We have an unusual scheduling for this migration and Newspaper2024BatchId is used to
  // decide the correct start date for each subscription.
  sealed trait Newspaper2024BatchId
  object MonthliesPart1 extends Newspaper2024BatchId // First batch of monthlies
  object MonthliesPart2 extends Newspaper2024BatchId // Second batch of monthlies
  object MoreThanMonthlies extends Newspaper2024BatchId // Quarterlies, Semi-Annuals and Annuals

  def subscriptionToMigrationProductName(subscription: ZuoraSubscription): Either[String, String] = {
    // We are doing a multi product migration. This function tries and retrieve the correct product given a
    // subscription.
    val migrationProductNames = List("Newspaper Delivery", "Newspaper Digital Voucher", "Newspaper Voucher")
    val names = subscription.ratePlans
      .filter(ratePlan => ratePlan.lastChangeType == None || ratePlan.lastChangeType == Some("Add"))
      .map(ratePlan => ratePlan.productName)
      .filter(name => migrationProductNames.contains(name))
      .distinct
    names match {
      case Nil =>
        Left(
          s"[error: d5fb6922] could not determine migration product name for subscription ${subscription.subscriptionNumber}; no name to choose from"
        )
      case name :: Nil => Right(name)
      case _ =>
        Left(
          s"[error: d3ecd18d] could not determine migration product name for subscription ${subscription.subscriptionNumber}; more than one name to choose from"
        )
    }
  }

  def ratePlanToBillingPeriod(ratePlan: ZuoraRatePlan): Option[BillingPeriod] = {
    for {
      ratePlanCharge <- ratePlan.ratePlanCharges.headOption
      billingPeriod <- ratePlanCharge.billingPeriod
    } yield BillingPeriod.fromString(billingPeriod)
  }

  def ratePlanToCurrency(ratePlan: ZuoraRatePlan): Option[String] = {
    for {
      ratePlanCharge <- ratePlan.ratePlanCharges.headOption
    } yield ratePlanCharge.currency
  }

  def subscriptionToRatePlanDetails(
      subscription: ZuoraSubscription,
      productName: String
  ): Either[String, RatePlanDetails] = {
    val ratePlans = {
      subscription.ratePlans
        .filter(ratePlan => ratePlan.productName == productName)
        .filter(ratePlan => ratePlan.lastChangeType == None || ratePlan.lastChangeType == Some("Add"))
        .distinct
    }
    ratePlans match {
      case Nil =>
        Left(
          s"[error 93a21a48] Subscription ${subscription.subscriptionNumber} was found to have zero newsPaperDeliveryRatePlans making determination of rate plan name impossible"
        )
      case ratePlan :: Nil => {
        (for {
          billingPeriod <- ratePlanToBillingPeriod(ratePlan)
          currency <- ratePlanToCurrency(ratePlan)
          currentPrice = ratePlan.ratePlanCharges.foldLeft(BigDecimal(0))(
            (price: BigDecimal, ratePlanCharge: ZuoraRatePlanCharge) =>
              price + ratePlanCharge.price.getOrElse(BigDecimal(0))
          )
        } yield RatePlanDetails(
          ratePlan,
          ratePlan.ratePlanName,
          billingPeriod,
          currency,
          currentPrice
        )) match {
          case Some(data) => Right(data)
          case _ => Left(s"[error: 0e218c37] Could not determine billing period for subscription ${subscription}")
        }
      }
      case _ =>
        Left(
          s"[error 93a21a48] Subscription ${subscription.subscriptionNumber} was found to have more than one newsPaperDeliveryRatePlans making determination of rate plan name impossible"
        )
    }
  }

  def subscriptionToNewPrice(subscription: ZuoraSubscription): Option[BigDecimal] = {
    for {
      productName <- subscriptionToMigrationProductName(subscription).toOption
      ratePlanDetails <- subscriptionToRatePlanDetails(subscription, productName).toOption
      price <- Newspaper2024MigrationStaticData.priceLookup(
        productName,
        ratePlanDetails.billingPeriod,
        ratePlanDetails.ratePlanName
      )
    } yield price
  }

  def priceData(
      subscription: ZuoraSubscription,
  ): Either[AmendmentDataFailure, PriceData] = {

    // PriceData(currency: Currency, oldPrice: BigDecimal, newPrice: BigDecimal, billingPeriod: String)

    def transform1[T](option: Option[T]): Either[AmendmentDataFailure, T] = {
      option match {
        case None                 => Left(AmendmentDataFailure("error"))
        case Some(ratePlanCharge) => Right(ratePlanCharge)
      }
    }

    def transform2[T](data: Either[String, T]): Either[AmendmentDataFailure, T] = {
      data match {
        case Left(string) => Left(AmendmentDataFailure(string))
        case Right(t)     => Right(t)
      }
    }

    for {
      productName <- transform2[String](subscriptionToMigrationProductName(subscription))
      ratePlanDetails <- transform2[RatePlanDetails](subscriptionToRatePlanDetails(subscription, productName))
      oldPrice = ratePlanDetails.currentPrice
      newPrice <- transform1[BigDecimal](subscriptionToNewPrice(subscription))
    } yield PriceData(
      ratePlanDetails.currency,
      oldPrice,
      newPrice,
      BillingPeriod.toString(ratePlanDetails.billingPeriod)
    )
  }

  def subscriptionToBatchId(subscription: ZuoraSubscription): Either[String, Newspaper2024BatchId] = {
    val ratePlanDetails = (for {
      productName <- subscriptionToMigrationProductName(subscription)
      ratePlanDetails <- subscriptionToRatePlanDetails(subscription, productName)
    } yield ratePlanDetails)

    ratePlanDetails match {
      case Left(message) => Left(message)
      case Right(ratePlanDetails) => {
        ratePlanDetails.billingPeriod match {
          case Monthly => {
            val ratePlan = ratePlanDetails.ratePlan
            ratePlan.ratePlanCharges.toList match {
              case Nil => Left("")
              case rpc :: _ => {
                val monthIndex = rpc.chargedThroughDate.getOrElse(LocalDate.of(2024, 1, 1)).getDayOfMonth
                if (monthIndex <= 20) {
                  Right(MonthliesPart2)
                } else {
                  Right(MonthliesPart1)
                }
              }
            }
          }
          case _ => Right(MoreThanMonthlies)
        }
      }
    }
  }

  def batchIdToEarliestMigrationStartDate(batchId: Newspaper2024BatchId): LocalDate = {
    batchId match {
      case MonthliesPart1    => LocalDate.of(2024, 2, 21) // 21 Feb 2024
      case MonthliesPart2    => LocalDate.of(2024, 3, 18) // 18 March 2024
      case MoreThanMonthlies => LocalDate.of(2024, 3, 1) // 1 March 2024
    }
  }

  def subscriptionToEarliestMigrationStartDate(subscription: ZuoraSubscription): LocalDate = {
    subscriptionToBatchId(subscription) match {
      case Right(bid)   => batchIdToEarliestMigrationStartDate(bid)
      case Left(string) => LocalDate.of(2024, 4, 1)
      // Default date to avoid returning a more complex value than a LocalDate
    }
  }

  def startDateGeneralLowerbound(
      cohortSpec: CohortSpec,
      today: LocalDate,
      subscription: ZuoraSubscription
  ): LocalDate = {

    // Technically the startDateGeneralLowerbound is a function of the cohort spec and the notification min time.
    // The cohort spec carries the lowest date we specify there can be a price migration, and the notification min
    // time ensures the legally required lead time for customer communication. The max of those two dates is the date
    // from which we can realistically perform a price increase. With that said, other policies can apply, for
    // instance:
    // - The one year policy, which demand that we do not price rise customers during the subscription first year
    // - The spread: a mechanism, used for monthlies, by which we do not let a large number of monthlies migrate
    //   during a single month.

    // We expanded the signature of this function for the Newspaper2024 migration where that date was specific of
    // the subscription due to its un-unusual scheduling. For Newspaper2024 we call a specific function from the
    // migration support code.

    val earliestPriceMigrationStartDate = subscriptionToEarliestMigrationStartDate(subscription)

    def datesMax(date1: LocalDate, date2: LocalDate): LocalDate = if (date1.isBefore(date2)) date2 else date1

    datesMax(
      earliestPriceMigrationStartDate,
      today.plusDays(
        Newspaper2024MigrationStaticData.minLeadTime + 1
      ) // +1 because we need to be strictly over minLeadTime days away. Exactly minLeadTime is not enough.
    )
  }

  def startDateSpreadPeriod(subscription: ZuoraSubscription): Int = {
    subscriptionToBatchId(subscription) match {
      case Left(_) => 1
      case Right(bid) =>
        bid match {
          case MonthliesPart1    => 1
          case MonthliesPart2    => 2
          case MoreThanMonthlies => 1
        }
    }
  }

  // Amendment supporting functions

  def subscriptionToNewPriceDistribution(
      subscription: ZuoraSubscription
  ): Option[Newspaper2024MigrationStaticData.PriceDistribution] = {
    for {
      productName <- subscriptionToMigrationProductName(subscription).toOption
      ratePlanDetails <- subscriptionToRatePlanDetails(subscription, productName).toOption
      priceDistribution <- Newspaper2024MigrationStaticData.priceDistributionLookup(
        productName,
        ratePlanDetails.billingPeriod,
        ratePlanDetails.ratePlanName
      )
    } yield priceDistribution
  }

}
