package pricemigrationengine.model

/*

Guardian Weekly Subs:

Payment Schedule | Localisation | Currency | GW Leg   | Digital Pack Leg | Total |

Month            | UK           |  GBP     |  60.5%   |           39.5%  | 100%  |
Month            | EU           |  EUR     |  67.9%   |           32.1%  | 100%  |
Month            | ROW          |  USD     |  57.7%   |           42.3%  | 100%  |
Month            | ROW          |  GBP     |  58.4%   |           41.6%  | 100%  |
Month            | US           |  USD     |  65.1%   |           34.9%  | 100%  |
Month            | CA           |  CAD     |  60.9%   |           39.1%  | 100%  |
Month            | AU           |  AUD     |  66.5%   |           33.5%  | 100%  |
Month            | NZ           |  NZD     |  68.7%   |           31.3%  | 100%  |
Quarter          | UK           |  GBP     |  60.5%   |           39.5%  | 100%  |
Quarter          | EU           |  EUR     |  67.9%   |           32.1%  | 100%  |
Quarter          | ROW          |  USD     |  57.7%   |           42.3%  | 100%  |
Quarter          | ROW          |  GBP     |  58.4%   |           41.6%  | 100%  |
Quarter          | US           |  USD     |  65.1%   |           34.9%  | 100%  |
Quarter          | CA           |  CAD     |  60.9%   |           39.1%  | 100%  |
Quarter          | AU           |  AUD     |  66.5%   |           33.5%  | 100%  |
Quarter          | NZ           |  NZD     |  68.7%   |           31.3%  | 100%  |
Semi Annual      | UK           |  GBP     |  60.5%   |           39.5%  | 100%  |
Semi Annual      | EU           |  EUR     |  67.9%   |           32.1%  | 100%  |
Semi Annual      | ROW          |  USD     |  57.7%   |           42.3%  | 100%  |
Semi Annual      | ROW          |  GBP     |  58.4%   |           41.6%  | 100%  |
Semi Annual      | US           |  USD     |  65.1%   |           34.9%  | 100%  |
Semi Annual      | CA           |  CAD     |  60.9%   |           39.1%  | 100%  |
Semi Annual      | AU           |  AUD     |  66.5%   |           33.5%  | 100%  |
Semi Annual      | NZ           |  NZD     |  68.7%   |           31.3%  | 100%  |
Annual           | UK           |  GBP     |  64.8%   |           35.2%  | 100%  |
Annual           | EU           |  EUR     |  71.7%   |           28.3%  | 100%  |
Annual           | ROW          |  USD     |  62.1%   |           37.9%  | 100%  |
Annual           | ROW          |  GBP     |  62.8%   |           37.2%  | 100%  |
Annual           | US           |  USD     |  69.2%   |           30.8%  | 100%  |
Annual           | CA           |  CAD     |  65.1%   |           34.9%  | 100%  |
Annual           | AU           |  AUD     |  70.4%   |           29.6%  | 100%  |
Annual           | NZ           |  NZD     |  72.5%   |           27.5%  | 100%  |
6 for 6          | UK           |  GBP     |  60.5%   |           39.5%  | 100%  |
6 for 6          | EU           |  EUR     |  67.9%   |           32.1%  | 100%  |
6 for 6          | ROW          |  USD     |  57.7%   |           42.3%  | 100%  |
6 for 6          | ROW          |  GBP     |  58.4%   |           41.6%  | 100%  |
6 for 6          | US           |  USD     |  65.1%   |           34.9%  | 100%  |
6 for 6          | CA           |  CAD     |  60.9%   |           39.1%  | 100%  |
6 for 6          | AU           |  AUD     |  66.5%   |           33.5%  | 100%  |
6 for 6          | NZ           |  NZD     |  68.7%   |           31.3%  | 100%  |

 */

// Here we define "GuardianWeeklyDeliverySchedule" instead of using
// a billing frequency because the finance data we are basing
// ourselves on doesn't have a clear mapping to billing periods. We have
// - Month
// - Quarter
// - Semi Annual
// - Annual
// - "6 for 6"
sealed trait T4xGuardianWeeklyPaymentSchedule
object T4xMonth extends T4xGuardianWeeklyPaymentSchedule
object T4xQuarter extends T4xGuardianWeeklyPaymentSchedule
object T4xSemiAnnual extends T4xGuardianWeeklyPaymentSchedule
object T4xAnnual extends T4xGuardianWeeklyPaymentSchedule
object T4xSixForSix extends T4xGuardianWeeklyPaymentSchedule

case class T5xDistribution(gardianWeeklyPercentage: BigDecimal, digitalPackPercentage: BigDecimal)

object GuardianWeeklyLegPercentageDistribution {
  val monthDistributions: Map[(Currency, PricingLocalisation), T5xDistribution] = Map(
    ("GBP", Domestic) -> T5xDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> T5xDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> T5xDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> T5xDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> T5xDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> T5xDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> T5xDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
  val quarterlyDistributions: Map[(Currency, PricingLocalisation), T5xDistribution] = Map(
    ("GBP", Domestic) -> T5xDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> T5xDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> T5xDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> T5xDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> T5xDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> T5xDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> T5xDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
  val semiAnnualDistributions: Map[(Currency, PricingLocalisation), T5xDistribution] = Map(
    ("GBP", Domestic) -> T5xDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> T5xDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> T5xDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> T5xDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> T5xDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> T5xDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> T5xDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
  val annualDistributions: Map[(Currency, PricingLocalisation), T5xDistribution] = Map(
    ("GBP", Domestic) -> T5xDistribution(BigDecimal(64.8), BigDecimal(35.2)),
    ("EUR", Domestic) -> T5xDistribution(BigDecimal(71.7), BigDecimal(28.3)),
    ("USD", RestOfWorld) -> T5xDistribution(BigDecimal(62.1), BigDecimal(37.9)),
    ("GBP", RestOfWorld) -> T5xDistribution(BigDecimal(62.8), BigDecimal(37.2)),
    ("USD", Domestic) -> T5xDistribution(BigDecimal(69.2), BigDecimal(30.8)),
    ("CAD", Domestic) -> T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("AUD", Domestic) -> T5xDistribution(BigDecimal(70.4), BigDecimal(29.6)),
    ("NZD", Domestic) -> T5xDistribution(BigDecimal(72.5), BigDecimal(27.5))
  )
  val sixForSixDistributions: Map[(Currency, PricingLocalisation), T5xDistribution] = Map(
    ("GBP", Domestic) -> T5xDistribution(BigDecimal(60.5), BigDecimal(39.5)),
    ("EUR", Domestic) -> T5xDistribution(BigDecimal(67.9), BigDecimal(32.1)),
    ("USD", RestOfWorld) -> T5xDistribution(BigDecimal(57.7), BigDecimal(42.3)),
    ("GBP", RestOfWorld) -> T5xDistribution(BigDecimal(58.4), BigDecimal(41.6)),
    ("USD", Domestic) -> T5xDistribution(BigDecimal(65.1), BigDecimal(34.9)),
    ("CAD", Domestic) -> T5xDistribution(BigDecimal(60.9), BigDecimal(39.1)),
    ("AUD", Domestic) -> T5xDistribution(BigDecimal(66.5), BigDecimal(33.5)),
    ("NZD", Domestic) -> T5xDistribution(BigDecimal(68.7), BigDecimal(31.3))
  )
}
