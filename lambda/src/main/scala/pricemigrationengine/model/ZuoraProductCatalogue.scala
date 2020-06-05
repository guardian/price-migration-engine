package pricemigrationengine.model

import java.time.LocalDate

import pricemigrationengine.model.Dates.isDateRangeCurrent
import upickle.default.{ReadWriter, macroRW}

case class ZuoraProductCatalogue(products: Set[ZuoraProduct], nextPage: Option[String] = None)

object ZuoraProductCatalogue {

  implicit val rw: ReadWriter[ZuoraProductCatalogue] = macroRW

  def empty: ZuoraProductCatalogue = ZuoraProductCatalogue(products = Set.empty)

  def productPricingMap(catalogue: ZuoraProductCatalogue): ZuoraPricingData = {
    def isActiveProduct(p: ZuoraProduct) = isDateRangeCurrent(p.effectiveStartDate, p.effectiveEndDate)
    def isActiveProductRatePlan(p: ZuoraProductRatePlan) = isDateRangeCurrent(p.effectiveStartDate, p.effectiveEndDate)
    val prices = for {
      product <- catalogue.products.filter(isActiveProduct)
      productRatePlan <- product.productRatePlans.filter(isActiveProductRatePlan)
      productRatePlanCharge <- productRatePlan.productRatePlanCharges
    } yield productRatePlanCharge.id -> productRatePlanCharge
    prices.toMap
  }
}

case class ZuoraProduct(
    productRatePlans: Set[ZuoraProductRatePlan],
    effectiveStartDate: LocalDate,
    effectiveEndDate: LocalDate
)

object ZuoraProduct {
  implicit val rw: ReadWriter[ZuoraProduct] = macroRW
}

case class ZuoraProductRatePlan(
    productRatePlanCharges: Set[ZuoraProductRatePlanCharge],
    effectiveStartDate: LocalDate,
    effectiveEndDate: LocalDate
)

object ZuoraProductRatePlan {
  implicit val rw: ReadWriter[ZuoraProductRatePlan] = macroRW
}

case class ZuoraProductRatePlanCharge(id: String, pricing: Set[ZuoraPricing])

object ZuoraProductRatePlanCharge {
  implicit val rw: ReadWriter[ZuoraProductRatePlanCharge] = macroRW
}

/*
 * Don't use discount percentage from product catalogue,
 * because it can be overridden so the default value is unreliable.
 */
case class ZuoraPricing(currency: Currency, price: Option[BigDecimal])

object ZuoraPricing {
  implicit val rw: ReadWriter[ZuoraPricing] = macroRW

  def pricing(productRatePlanCharge: ZuoraProductRatePlanCharge, currency: Currency): Option[ZuoraPricing] =
    productRatePlanCharge.pricing.find(_.currency == currency)
}
