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
      ratePlan <- product.productRatePlans.filter(isActiveProductRatePlan)
      ratePlanCharge <- ratePlan.productRatePlanCharges
      pricing <- ratePlanCharge.pricing
    } yield ratePlanCharge.id -> pricing
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

case class ZuoraPricing(currency: String, price: Option[BigDecimal], discountPercentage: Double)

object ZuoraPricing {
  implicit val rw: ReadWriter[ZuoraPricing] = macroRW
}
