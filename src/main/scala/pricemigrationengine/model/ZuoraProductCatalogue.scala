package pricemigrationengine.model

import upickle.default.{ReadWriter, macroRW}

case class ZuoraProductCatalogue(products: Set[ZuoraProduct])

object ZuoraProductCatalogue {

  implicit val rw: ReadWriter[ZuoraProductCatalogue] = macroRW

  def productPricingMap(catalogue: ZuoraProductCatalogue): ZuoraPricingData = {
    val prices = for {
      product <- catalogue.products
      ratePlan <- product.productRatePlans
      ratePlanCharge <- ratePlan.productRatePlanCharges
      pricing <- ratePlanCharge.pricing
    } yield ratePlanCharge.id -> pricing
    prices.toMap
  }
}

case class ZuoraProduct(productRatePlans: Set[ZuoraProductRatePlan])

object ZuoraProduct {
  implicit val rw: ReadWriter[ZuoraProduct] = macroRW
}

case class ZuoraProductRatePlan(productRatePlanCharges: Set[ZuoraProductRatePlanCharge])

object ZuoraProductRatePlan {
  implicit val rw: ReadWriter[ZuoraProductRatePlan] = macroRW
}

case class ZuoraProductRatePlanCharge(id: String, pricing: Set[ZuoraPricing])

object ZuoraProductRatePlanCharge {
  implicit val rw: ReadWriter[ZuoraProductRatePlanCharge] = macroRW
}

case class ZuoraPricing(currency: String, price: BigDecimal)

object ZuoraPricing {
  implicit val rw: ReadWriter[ZuoraPricing] = macroRW
}
