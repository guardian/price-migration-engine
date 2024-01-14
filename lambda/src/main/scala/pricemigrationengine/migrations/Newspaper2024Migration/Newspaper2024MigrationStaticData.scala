package pricemigrationengine.migrations
import pricemigrationengine.model._

import java.time.LocalDate

object Newspaper2024MigrationStaticData {

  val maxLeadTime = 40
  val minLeadTime = 35

  def ratePlanIdLookUp(product: String, rateplanName: String): Option[String] = {
    (product, rateplanName) match {
      case ("Newspaper Delivery", "Everyday")  => Some("2c92a0fd560d13880156136b72e50f0c")
      case ("Newspaper Delivery", "Sixday")    => Some("2c92a0ff560d311b0156136f2afe5315")
      case ("Newspaper Delivery", "Weekend")   => Some("2c92a0fd5614305c01561dc88f3275be")
      case ("Newspaper Delivery", "Saturday")  => Some("2c92a0fd5e1dcf0d015e3cb39d0a7ddb")
      case ("Newspaper Delivery", "Sunday")    => Some("2c92a0ff5af9b657015b0fea5b653f81")
      case ("Newspaper Delivery", "Everyday+") => Some("2c92a0fd560d132301560e43cf041a3c")
      case ("Newspaper Delivery", "Sixday+")   => Some("2c92a0ff560d311b0156136b697438a9")
      case ("Newspaper Delivery", "Weekend+")  => Some("2c92a0ff560d311b0156136b9f5c3968")
      case ("Newspaper Delivery", "Saturday+") => Some("2c92a0ff6205708e01622484bb2c4613")
      case ("Newspaper Delivery", "Sunday+")   => Some("2c92a0fd560d13880156136b8e490f8b")

      case ("Newspaper Digital Voucher", "Everyday")  => Some("2c92a00870ec598001710740c78d2f13")
      case ("Newspaper Digital Voucher", "Sixday")    => Some("2c92a00870ec598001710740ca532f69")
      case ("Newspaper Digital Voucher", "Weekend")   => Some("2c92a00870ec598001710740d24b3022")
      case ("Newspaper Digital Voucher", "Saturday")  => Some("2c92a00870ec598001710740cdd02fbd")
      case ("Newspaper Digital Voucher", "Sunday")    => Some("2c92a00870ec598001710740d0d83017")
      case ("Newspaper Digital Voucher", "Everyday+") => Some("2c92a00870ec598001710740d3d03035")
      case ("Newspaper Digital Voucher", "Sixday+")   => Some("2c92a00870ec598001710740c4582ead")
      case ("Newspaper Digital Voucher", "Weekend+")  => Some("2c92a00870ec598001710740c6672ee7")
      case ("Newspaper Digital Voucher", "Saturday+") => Some("2c92a00870ec598001710740ce702ff0")
      case ("Newspaper Digital Voucher", "Sunday+")   => Some("2c92a00870ec598001710740cf9e3004")

      case ("Newspaper Voucher", "Everyday")  => Some("2c92a0fd56fe270b0157040dd79b35da")
      case ("Newspaper Voucher", "Sixday")    => Some("2c92a0fd56fe270b0157040e42e536ef")
      case ("Newspaper Voucher", "Weekend")   => Some("2c92a0ff56fe33f00157040f9a537f4b")
      case ("Newspaper Voucher", "Saturday")  => Some("2c92a0fd6205707201621f9f6d7e0116")
      case ("Newspaper Voucher", "Sunday")    => Some("2c92a0fe5af9a6b9015b0fe1ecc0116c")
      case ("Newspaper Voucher", "Everyday+") => Some("2c92a0ff56fe33f50157040bbdcf3ae4")
      case ("Newspaper Voucher", "Sixday+")   => Some("2c92a0fc56fe26ba0157040c5ea17f6a")
      case ("Newspaper Voucher", "Weekend+")  => Some("2c92a0fd56fe26b60157040cdd323f76")
      case ("Newspaper Voucher", "Saturday+") => Some("2c92a0fd6205707201621fa1350710e3")
      case ("Newspaper Voucher", "Sunday+")   => Some("2c92a0fe56fe33ff0157040d4b824168")
      case _                                  => None
    }
  }

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

  // case class RatePlanCharge2024(price: BigDecimal)

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

  case class IndividualCharge2024(chargeId: String, Price: BigDecimal)

  case class ChargeDistribution2024(
      monday: Option[IndividualCharge2024],
      tuesday: Option[IndividualCharge2024],
      wednesday: Option[IndividualCharge2024],
      thursday: Option[IndividualCharge2024],
      friday: Option[IndividualCharge2024],
      saturday: Option[IndividualCharge2024],
      sunday: Option[IndividualCharge2024],
      digitalPack: Option[IndividualCharge2024]
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

  def chargeDistributionMultiplier(
      chargeDistribution: ChargeDistribution2024,
      multiplier: Int
  ): ChargeDistribution2024 = {
    def mult(ic: IndividualCharge2024, multiplier: Int): IndividualCharge2024 =
      IndividualCharge2024(ic.chargeId, ic.Price * BigDecimal(multiplier))
    ChargeDistribution2024(
      monday = chargeDistribution.monday.map(ic => mult(ic, multiplier)),
      tuesday = chargeDistribution.tuesday.map(ic => mult(ic, multiplier)),
      wednesday = chargeDistribution.wednesday.map(ic => mult(ic, multiplier)),
      thursday = chargeDistribution.thursday.map(ic => mult(ic, multiplier)),
      friday = chargeDistribution.friday.map(ic => mult(ic, multiplier)),
      saturday = chargeDistribution.saturday.map(ic => mult(ic, multiplier)),
      sunday = chargeDistribution.sunday.map(ic => mult(ic, multiplier)),
      digitalPack = chargeDistribution.digitalPack.map(ic => mult(ic, multiplier))
    )
  }

  val newspaperHomeDeliveryMonthlyChargeDistributionsMap: Map[String, ChargeDistribution2024] = Map(
    "Everyday" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a0fd560d13880156136b74340f36", BigDecimal(10.24))),
      tuesday = Some(IndividualCharge2024("2c92a0fd560d13880156136b74780f3f", BigDecimal(10.24))),
      wednesday = Some(IndividualCharge2024("2c92a0fd560d13880156136b730d0f0e", BigDecimal(10.24))),
      thursday = Some(IndividualCharge2024("2c92a0fd560d13880156136b73b50f26", BigDecimal(10.24))),
      friday = Some(IndividualCharge2024("2c92a0fd560d13880156136b73770f1e", BigDecimal(10.24))),
      saturday = Some(IndividualCharge2024("2c92a0fd560d13880156136b74b80f47", BigDecimal(13.89))),
      sunday = Some(IndividualCharge2024("2c92a0fd560d13230156137061435de7", BigDecimal(13.90))),
      digitalPack = None,
    ),
    "Sixday" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a0ff560d311b0156136f2bc2532f", BigDecimal(10.85))),
      tuesday = Some(IndividualCharge2024("2c92a0ff560d311b0156136f2c015337", BigDecimal(10.85))),
      wednesday = Some(IndividualCharge2024("2c92a0ff560d311b0156136f2b185317", BigDecimal(10.85))),
      thursday = Some(IndividualCharge2024("2c92a0ff560d311b0156136f2b8c5327", BigDecimal(10.85))),
      friday = Some(IndividualCharge2024("2c92a0ff560d311b0156136f2b50531f", BigDecimal(10.85))),
      saturday = Some(IndividualCharge2024("2c92a0ff560d311b0156136f2c43533f", BigDecimal(14.74))),
      sunday = None,
      digitalPack = None,
    ),
    "Weekend" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a0fd5614305c01561dc88fb875d0", BigDecimal(15.99))),
      Some(IndividualCharge2024("2c92a0fd5614305c01561dc88f8975c8", BigDecimal(16.00))),
      None
    ),
    "Saturday" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a0fd5e1dcf0d015e3cb39d207ddf", BigDecimal(19.99))),
      None,
    ),
    "Sunday" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a0ff5af9b657015b0fea5bb83fa8", BigDecimal(19.99))),
      None,
    ),
    "Everyday+" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a0ff560d31190156134be59060f4", BigDecimal(10.24))),
      tuesday = Some(IndividualCharge2024("2c92a0ff560d311b015613623e050a63", BigDecimal(10.24))),
      wednesday = Some(IndividualCharge2024("2c92a0fc560d13390156136324931d21", BigDecimal(10.24))),
      thursday = Some(IndividualCharge2024("2c92a0fe560d3104015613640f555223", BigDecimal(10.24))),
      friday = Some(IndividualCharge2024("2c92a0fd560d138801561364cad96af7", BigDecimal(10.24))),
      saturday = Some(IndividualCharge2024("2c92a0ff560d311c0156136573e366f3", BigDecimal(13.89))),
      sunday = Some(IndividualCharge2024("2c92a0fe560d31040156136626dd5d1b", BigDecimal(13.90))),
      digitalPack = Some(IndividualCharge2024("2c92a0fd560d132901561367b2f17763", BigDecimal(2.00))),
    ),
    "Sixday+" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a0ff560d311b0156136b6ac738d5", BigDecimal(10.85))),
      tuesday = Some(IndividualCharge2024("2c92a0ff560d311b0156136b6b0438dd", BigDecimal(10.85))),
      wednesday = Some(IndividualCharge2024("2c92a0ff560d311b0156136b698f38ac", BigDecimal(10.85))),
      thursday = Some(IndividualCharge2024("2c92a0ff560d311b0156136b6a4138c5", BigDecimal(10.85))),
      friday = Some(IndividualCharge2024("2c92a0ff560d311b0156136b6a0838bc", BigDecimal(10.85))),
      saturday = Some(IndividualCharge2024("2c92a0ff560d311b0156136b6b4b38e6", BigDecimal(14.74))),
      sunday = None,
      digitalPack = Some(IndividualCharge2024("2c92a0ff560d311b0156136b69d038b4", BigDecimal(2.00))),
    ),
    "Weekend+" -> ChargeDistribution2024(
      monday = None,
      tuesday = None,
      wednesday = None,
      thursday = None,
      friday = None,
      saturday = Some(IndividualCharge2024("2c92a0ff560d311b0156136ba11539ae", BigDecimal(15.99))),
      sunday = Some(IndividualCharge2024("2c92a0ff560d311b0156136ba0523996", BigDecimal(16.00))),
      digitalPack = Some(IndividualCharge2024("2c92a0ff560d311b0156136b9fac3976", BigDecimal(9.00))),
    ),
    "Saturday+" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a0ff6205708e01622484bb68461d", BigDecimal(19.99))),
      digitalPack = Some(IndividualCharge2024("2c92a0ff6205708e01622484bb404615", BigDecimal(11.00))),
    ),
    "Sunday+" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a0fd560d13880156136b8f5d0fba", BigDecimal(19.99))),
      digitalPack = Some(IndividualCharge2024("2c92a0fd560d13880156136b8e9f0f99", BigDecimal(11.00))),
    ),
  )

  val newspaperHomeDeliveryQuarterlyChargeDistributionsMap: Map[String, ChargeDistribution2024] = Map(
    "Everyday" -> chargeDistributionMultiplier(newspaperHomeDeliveryMonthlyChargeDistributionsMap("Everyday"), 3),
    "Sixday" -> chargeDistributionMultiplier(newspaperHomeDeliveryMonthlyChargeDistributionsMap("Sixday"), 3),
    "Weekend" -> chargeDistributionMultiplier(newspaperHomeDeliveryMonthlyChargeDistributionsMap("Weekend"), 3),
    "Saturday" -> chargeDistributionMultiplier(newspaperHomeDeliveryMonthlyChargeDistributionsMap("Saturday"), 3),
    "Sunday" -> chargeDistributionMultiplier(newspaperHomeDeliveryMonthlyChargeDistributionsMap("Sunday"), 3),
  )

  val newspaperSubscriptionCardMonthlyChargeDistributionsMap: Map[String, ChargeDistribution2024] = Map(
    "Everyday" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a00870ec598001710740c7b82f1c", BigDecimal(8.42))),
      tuesday = Some(IndividualCharge2024("2c92a00870ec598001710740c80f2f26", BigDecimal(8.42))),
      wednesday = Some(IndividualCharge2024("2c92a00870ec598001710740c9802f59", BigDecimal(8.42))),
      thursday = Some(IndividualCharge2024("2c92a00870ec598001710740c8c42f40", BigDecimal(8.42))),
      friday = Some(IndividualCharge2024("2c92a00870ec598001710740c91d2f4d", BigDecimal(8.42))),
      saturday = Some(IndividualCharge2024("2c92a00870ec598001710740c8652f37", BigDecimal(11.44))),
      sunday = Some(IndividualCharge2024("2c92a00870ec598001710740c9d72f61", BigDecimal(11.45))),
      digitalPack = None,
    ),
    "Sixday" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a00870ec598001710740cbb32f77", BigDecimal(8.96))),
      tuesday = Some(IndividualCharge2024("2c92a00870ec598001710740cc2c2f80", BigDecimal(8.96))),
      wednesday = Some(IndividualCharge2024("2c92a00870ec598001710740cd012f90", BigDecimal(8.96))),
      thursday = Some(IndividualCharge2024("2c92a00870ec598001710740cc9b2f88", BigDecimal(8.96))),
      friday = Some(IndividualCharge2024("2c92a00870ec598001710740cb4e2f6b", BigDecimal(8.96))),
      saturday = Some(IndividualCharge2024("2c92a00870ec598001710740cd6e2fa2", BigDecimal(12.19))),
      sunday = None,
      digitalPack = None,
    ),
    "Weekend" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a00870ec598001710740d28e3024", BigDecimal(12.99))),
      Some(IndividualCharge2024("2c92a00870ec598001710740d325302c", BigDecimal(13.00))),
      None
    ),
    "Saturday" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a00870ec598001710740ce042fcb", BigDecimal(14.99))),
      None,
    ),
    "Sunday" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a00870ec598001710740d1103019", BigDecimal(14.99))),
      digitalPack = None,
    ),
    "Everyday+" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a00870ec598001710740d5fd3073", BigDecimal(8.42))),
      tuesday = Some(IndividualCharge2024("2c92a00870ec598001710740d54f3069", BigDecimal(8.42))),
      wednesday = Some(IndividualCharge2024("2c92a00870ec598001710740d7493084", BigDecimal(8.42))),
      thursday = Some(IndividualCharge2024("2c92a00870ec598001710740d691307c", BigDecimal(8.42))),
      friday = Some(IndividualCharge2024("2c92a00870ec598001710740d8873096", BigDecimal(8.42))),
      saturday = Some(IndividualCharge2024("2c92a00870ec598001710740d4b8304f", BigDecimal(11.44))),
      sunday = Some(IndividualCharge2024("2c92a00870ec598001710740d7e2308d", BigDecimal(11.45))),
      digitalPack = Some(IndividualCharge2024("2c92a00870ec598001710740d4143037", BigDecimal(2.00))),
    ),
    "Sixday+" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a00870ec598001710740c5962ecf", BigDecimal(8.96))),
      tuesday = Some(IndividualCharge2024("2c92a00870ec598001710740c60f2edf", BigDecimal(8.96))),
      wednesday = Some(IndividualCharge2024("2c92a00870ec598001710740c4dc2eb7", BigDecimal(8.96))),
      thursday = Some(IndividualCharge2024("2c92a00870ec598001710740c48e2eaf", BigDecimal(8.96))),
      friday = Some(IndividualCharge2024("2c92a00870ec598001710740c5192ebf", BigDecimal(8.96))),
      saturday = Some(IndividualCharge2024("2c92a00870ec598001710740c55a2ec7", BigDecimal(12.19))),
      sunday = None,
      digitalPack = Some(IndividualCharge2024("2c92a00870ec598001710740c5cf2ed7", BigDecimal(2.00))),
    ),
    "Weekend+" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a00870ec598001710740c6872ee9", BigDecimal(12.99))),
      Some(IndividualCharge2024("2c92a00870ec598001710740c7132efe", BigDecimal(13.00))),
      digitalPack = Some(IndividualCharge2024("2c92a00870ec598001710740c6ce2ef1", BigDecimal(9.00))),
    ),
    "Saturday+" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a00870ec598001710740cf1e2ffc", BigDecimal(14.99))),
      digitalPack = Some(IndividualCharge2024("2c92a00870ec598001710740cea02ff4", BigDecimal(11.00))),
    ),
    "Sunday+" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a00870ec598001710740d053300f", BigDecimal(14.99))),
      digitalPack = Some(IndividualCharge2024("2c92a00870ec598001710740cfda3006", BigDecimal(11.00))),
    ),
  )

  val newspaperSubscriptionCardQuarterlyChargeDistributionsMap: Map[String, ChargeDistribution2024] = Map(
    "Everyday" -> chargeDistributionMultiplier(newspaperSubscriptionCardMonthlyChargeDistributionsMap("Everyday"), 3),
    "Sixday" -> chargeDistributionMultiplier(newspaperSubscriptionCardMonthlyChargeDistributionsMap("Sixday"), 3),
    "Weekend" -> chargeDistributionMultiplier(newspaperSubscriptionCardMonthlyChargeDistributionsMap("Weekend"), 3),
    "Everyday+" -> chargeDistributionMultiplier(newspaperSubscriptionCardMonthlyChargeDistributionsMap("Everyday+"), 3),
    "Sixday+" -> chargeDistributionMultiplier(newspaperSubscriptionCardMonthlyChargeDistributionsMap("Sixday+"), 3),
  )

  val newspaperSubscriptionCardSemiAnnualChargeDistributionsMap: Map[String, ChargeDistribution2024] = Map(
    "Everyday" -> chargeDistributionMultiplier(newspaperSubscriptionCardMonthlyChargeDistributionsMap("Everyday"), 6),
    "Sixday" -> chargeDistributionMultiplier(newspaperSubscriptionCardMonthlyChargeDistributionsMap("Sixday"), 6),
    "Everyday+" -> chargeDistributionMultiplier(newspaperSubscriptionCardMonthlyChargeDistributionsMap("Everyday+"), 6),
  )

  val newspaperSubscriptionCardAnnualChargeDistributionsMap: Map[String, ChargeDistribution2024] = Map(
    "Everyday" -> chargeDistributionMultiplier(newspaperSubscriptionCardMonthlyChargeDistributionsMap("Everyday"), 12),
    "Sixday" -> chargeDistributionMultiplier(newspaperSubscriptionCardMonthlyChargeDistributionsMap("Sixday"), 12),
    "Weekend" -> chargeDistributionMultiplier(newspaperSubscriptionCardMonthlyChargeDistributionsMap("Weekend"), 12),
  )

  val newspaperVoucherBookMonthlChargeDistibutionsMap: Map[String, ChargeDistribution2024] = Map(
    "Everyday" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a0fd56fe26b601570431a5bc5a34", BigDecimal(8.42))),
      tuesday = Some(IndividualCharge2024("2c92a0fd56fe26b6015709c0613b44a6", BigDecimal(8.42))),
      wednesday = Some(IndividualCharge2024("2c92a0ff56fe33f3015709c110a71630", BigDecimal(8.42))),
      thursday = Some(IndividualCharge2024("2c92a0ff56fe33f0015709c182cb7c82", BigDecimal(8.42))),
      friday = Some(IndividualCharge2024("2c92a0ff56fe33f0015709c215527db4", BigDecimal(8.42))),
      saturday = Some(IndividualCharge2024("2c92a0fd56fe270b015709c320ee0595", BigDecimal(11.44))),
      sunday = Some(IndividualCharge2024("2c92a0ff56fe33f5015709c39719783e", BigDecimal(11.45))),
      digitalPack = None,
    ),
    "Sixday" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a0fd56fe26b601570431210a310e", BigDecimal(8.96))),
      tuesday = Some(IndividualCharge2024("2c92a0fd56fe270b015709bd2d3d75d7", BigDecimal(8.96))),
      wednesday = Some(IndividualCharge2024("2c92a0fe56fe33ff015709bdb6153cd4", BigDecimal(8.96))),
      thursday = Some(IndividualCharge2024("2c92a0fd56fe270b015709be701e78b6", BigDecimal(8.96))),
      friday = Some(IndividualCharge2024("2c92a0fc56fe26ba015709bee15d653a", BigDecimal(8.96))),
      saturday = Some(IndividualCharge2024("2c92a0ff56fe33f5015709bf7fdd6a4d", BigDecimal(12.19))),
      sunday = None,
      digitalPack = None,
    ),
    "Weekend" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a0ff56fe33f00157041713362b51", BigDecimal(12.99))),
      Some(IndividualCharge2024("2c92a0fc56fe26ba01570417df6d1b54", BigDecimal(13.00))),
      None
    ),
    "Saturday" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a0fd6205707201621f9f6d8d0118", BigDecimal(14.99))),
      None,
    ),
    "Sunday" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a0fe5af9a6b9015b0fe1ed121177", BigDecimal(14.99))),
      digitalPack = None,
    ),
    "Everyday+" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a0fd56fe270b015709c90c291c49", BigDecimal(8.42))),
      tuesday = Some(IndividualCharge2024("2c92a0fd56fe26b6015709ca144a646a", BigDecimal(8.42))),
      wednesday = Some(IndividualCharge2024("2c92a0ff56fe33f0015709cac4561bf3", BigDecimal(8.42))),
      thursday = Some(IndividualCharge2024("2c92a0fd56fe270b015709cc16f92645", BigDecimal(8.42))),
      friday = Some(IndividualCharge2024("2c92a0ff56fe33f5015709cce7ad1aea", BigDecimal(8.42))),
      saturday = Some(IndividualCharge2024("2c92a0fd56fe26b60157042fcd462666", BigDecimal(11.44))),
      sunday = Some(IndividualCharge2024("2c92a0ff56fe33f5015709c80af30495", BigDecimal(11.45))),
      digitalPack = Some(IndividualCharge2024("2c92a0fc56fe26ba01570418eddd26e1", BigDecimal(2.00))),
    ),
    "Sixday+" -> ChargeDistribution2024(
      monday = Some(IndividualCharge2024("2c92a0fe56fe33ff015704325d87494c", BigDecimal(8.96))),
      tuesday = Some(IndividualCharge2024("2c92a0ff56fe33f5015709cdedbd246b", BigDecimal(8.96))),
      wednesday = Some(IndividualCharge2024(" 2c92a0fd56fe26b6015709ced61a032e", BigDecimal(8.96))),
      thursday = Some(IndividualCharge2024(" 2c92a0fc56fe26ba015709cf4bbd3d1c", BigDecimal(8.96))),
      friday = Some(IndividualCharge2024("2c92a0fd56fe26b6015709cfc1500a2e", BigDecimal(8.96))),
      saturday = Some(IndividualCharge2024("2c92a0fd56fe26b6015709d078df4a80", BigDecimal(12.19))),
      sunday = None,
      digitalPack = Some(IndividualCharge2024("2c92a0ff56fe33f3015709d10a436f52", BigDecimal(2.00))),
    ),
    "Weekend+" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a0fd56fe26b601570432f4e33d17", BigDecimal(12.99))),
      Some(IndividualCharge2024("2c92a0ff56fe33f5015709b8fc4d5617", BigDecimal(13.00))),
      digitalPack = Some(IndividualCharge2024("2c92a0fe56fe33ff015709bb986636d8", BigDecimal(9.00))),
    ),
    "Saturday+" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a0fd6205707201621fa1354710ed", BigDecimal(14.99))),
      digitalPack = Some(IndividualCharge2024("2c92a0fd6205707201621fa1351710e5", BigDecimal(11.00))),
    ),
    "Sunday+" -> ChargeDistribution2024(
      None,
      None,
      None,
      None,
      None,
      None,
      Some(IndividualCharge2024("2c92a0fd56fe26b601570433b108633c", BigDecimal(14.99))),
      digitalPack = Some(IndividualCharge2024("2c92a0fc56fe26ba015709b7b7b04a2a", BigDecimal(11.00))),
    ),
  )

  val newspaperVoucherBookQuarterlyChargeDistibutionsMap: Map[String, ChargeDistribution2024] = Map(
    "Everyday" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Everyday"), 3),
    "Sixday" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Sixday"), 3),
    "Weekend" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Weekend"), 3),
    "Everyday+" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Everyday+"), 3),
    "Sixday+" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Sixday+"), 3),
    "Weekend+" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Weekend+"), 3),
    "Sunday+" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Sunday+"), 3),
  )

  val newspaperVoucherBookSemiAnnualChargeDistributionsMap: Map[String, ChargeDistribution2024] = Map(
    "Everyday" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Everyday"), 6),
    "Sixday" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Sixday"), 6),
    "Weekend" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Weekend"), 6),
    "Everyday+" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Everyday+"), 6),
    "Sixday+" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Sixday+"), 6),
    "Weekend+" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Weekend+"), 6),
    "Sunday+" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Sunday+"), 6),
  )

  val newspaperVoucherBookAnnualChargeDistributionsMap: Map[String, ChargeDistribution2024] = Map(
    "Everyday" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Everyday"), 12),
    "Sixday" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Sixday"), 12),
    "Weekend" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Weekend"), 12),
    "Everyday+" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Everyday+"), 12),
    "Sixday+" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Sixday+"), 12),
    "Weekend+" -> chargeDistributionMultiplier(newspaperVoucherBookMonthlChargeDistibutionsMap("Weekend+"), 12),
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
  ): Option[ChargeDistribution2024] = {
    val empty: Map[String, ChargeDistribution2024] = Map()
    val priceMap = (product, billingPeriod) match {
      case ("Newspaper Delivery", Monthly)           => newspaperHomeDeliveryMonthlyChargeDistributionsMap
      case ("Newspaper Delivery", Quarterly)         => newspaperHomeDeliveryQuarterlyChargeDistributionsMap
      case ("Newspaper Digital Voucher", Monthly)    => newspaperSubscriptionCardMonthlyChargeDistributionsMap
      case ("Newspaper Digital Voucher", Quarterly)  => newspaperSubscriptionCardQuarterlyChargeDistributionsMap
      case ("Newspaper Digital Voucher", SemiAnnual) => newspaperSubscriptionCardSemiAnnualChargeDistributionsMap
      case ("Newspaper Digital Voucher", Annual)     => newspaperSubscriptionCardAnnualChargeDistributionsMap
      case ("Newspaper Voucher", Monthly)            => newspaperVoucherBookMonthlChargeDistibutionsMap
      case ("Newspaper Voucher", Quarterly)          => newspaperVoucherBookQuarterlyChargeDistibutionsMap
      case ("Newspaper Voucher", SemiAnnual)         => newspaperVoucherBookSemiAnnualChargeDistributionsMap
      case ("Newspaper Voucher", Annual)             => newspaperVoucherBookAnnualChargeDistributionsMap
      case _                                         => empty
    }
    priceMap.get(ratePlanName)
  }

  def chargeDistributionToPrice(distribution: ChargeDistribution2024): BigDecimal = {
    List(
      distribution.monday.map(ic => ic.Price).getOrElse(BigDecimal(0)),
      distribution.tuesday.map(ic => ic.Price).getOrElse(BigDecimal(0)),
      distribution.wednesday.map(ic => ic.Price).getOrElse(BigDecimal(0)),
      distribution.thursday.map(ic => ic.Price).getOrElse(BigDecimal(0)),
      distribution.friday.map(ic => ic.Price).getOrElse(BigDecimal(0)),
      distribution.saturday.map(ic => ic.Price).getOrElse(BigDecimal(0)),
      distribution.sunday.map(ic => ic.Price).getOrElse(BigDecimal(0)),
      distribution.digitalPack.map(ic => ic.Price).getOrElse(BigDecimal(0))
    ).foldLeft(BigDecimal(0))((sum, item) => sum + item)
  }
}
