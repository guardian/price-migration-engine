package pricemigrationengine.model

import upickle.default.{ReadWriter, macroRW}

case class ZuoraProductCatalogue(products: Set[ZuoraProduct], nextPage: Option[String] = None)

object ZuoraProductCatalogue {
  implicit val rw: ReadWriter[ZuoraProductCatalogue] = macroRW
  def empty: ZuoraProductCatalogue = ZuoraProductCatalogue(products = Set.empty)
}

case class ZuoraProduct(
    name: String,
    productRatePlans: Set[ZuoraProductRatePlan]
)

object ZuoraProduct {
  implicit val rw: ReadWriter[ZuoraProduct] = macroRW
}

case class ZuoraProductRatePlan(
    id: String,
    name: String,
    status: String,
    productRatePlanCharges: Set[ZuoraProductRatePlanCharge]
)

object ZuoraProductRatePlan {
  implicit val rw: ReadWriter[ZuoraProductRatePlan] = macroRW
}

case class ZuoraProductRatePlanCharge(id: String, billingPeriod: Option[String], pricing: Set[ZuoraPricing])

object ZuoraProductRatePlanCharge {
  implicit val rw: ReadWriter[ZuoraProductRatePlanCharge] = macroRW
}

/*
 * Don't use discount percentage from product catalogue,
 * because it can be overridden so the default value is unreliable.
 */
case class ZuoraPricing(currency: Currency, price: Option[BigDecimal], hasBeenPriceCapped: Boolean = false)

object ZuoraPricing {
  implicit val rw: ReadWriter[ZuoraPricing] = macroRW
}
