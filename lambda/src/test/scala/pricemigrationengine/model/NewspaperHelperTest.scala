package pricemigrationengine.model

import pricemigrationengine.Fixtures

import java.time.LocalDate

// Newspaper:
// sub1: "Newspaper Voucher"          "Everyday+"   "GBP"   "Month"
// val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelper/sub1/subscription.json")
// val account = Fixtures.accountFromJson("model/NewspaperHelper/sub1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("model/NewspaperHelper/sub1/invoice-preview.json")

// sub2:  "Newspaper Digital Voucher"  "Everyday+"
// sub3:  "Newspaper Delivery"         "Everyday+"
// sub4:  "Newspaper Voucher"          "Sixday+"
// sub5:  "Newspaper Voucher"          "Weekend+"    "GBP"   "Month"
// sub6:  "Newspaper Voucher"          "Everyday"
// sub7:  "Newspaper Voucher"          "Sixday"
// sub8:  "Newspaper Voucher"          "Sixday+"     "GBP"   "Quarter"
// sub9:  "Newspaper Voucher"          "Everyday+"   "GBP"   "Annual"
// sub10: "Newspaper Voucher"          "Everyday+"   "GBP"   "Annual"
//        special edition of sub9 to test the 7.1% price cap
//        charges sum to 100 GBP
// sub11: "Newspaper - National Delivery" "Weekend"     "GBP"   "Month"
// sub12: "Newspaper Delivery"            "Echo-Legacy" "GBP"   "Month" (has sunday)

// sub13: variant of sub12, without the Sunday
//        More exactly it has the Sunday leg, but I set the price to zero

class NewspaperHelperTest extends munit.FunSuite {

  // --------------------

  test("NewspaperHelper.subscriptionToT3xDeliveryCategory") {
    val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelper/sub1/subscription.json")
    val today = LocalDate.of(2026, 9, 1)
    assertEquals(
      NewspaperHelper.subscriptionToT3xDeliveryCategory(subscription, today),
      Right(T3xNewspaperVoucher)
    )
  }
  test("NewspaperHelper.subscriptionToT3xDeliveryCategory") {
    val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelper/sub2/subscription.json")
    val today = LocalDate.of(2026, 9, 1)
    assertEquals(
      NewspaperHelper.subscriptionToT3xDeliveryCategory(subscription, today),
      Right(T3xNewspaperDigitalVoucher)
    )
  }
  test("NewspaperHelper.subscriptionToT3xDeliveryCategory") {
    val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelper/sub11/subscription.json")
    val today = LocalDate.of(2026, 9, 1)
    assertEquals(
      NewspaperHelper.subscriptionToT3xDeliveryCategory(subscription, today),
      Right(T3xNewspaperNationalDelivery)
    )
  }
  test("NewspaperHelper.subscriptionToT3xDeliveryCategory") {
    val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelper/sub12/subscription.json")
    val today = LocalDate.of(2026, 9, 1)
    assertEquals(
      NewspaperHelper.subscriptionToT3xDeliveryCategory(subscription, today),
      Right(T3xNewspaperDelivery)
    )
  }

  // --------------------

  test("NewspaperHelper.subscriptionToT2xNewspaperPackage") {
    val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelper/sub1/subscription.json")
    val today = LocalDate.of(2026, 9, 1)
    assertEquals(
      NewspaperHelper.subscriptionToT2xNewspaperPackage(subscription, today),
      Right(T2xEverydayPlus)
    )
  }
  test("NewspaperHelper.subscriptionToT2xNewspaperPackage") {
    val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelper/sub4/subscription.json")
    val today = LocalDate.of(2026, 9, 1)
    assertEquals(
      NewspaperHelper.subscriptionToT2xNewspaperPackage(subscription, today),
      Right(T2xSixDayPlus)
    )
  }
  test("NewspaperHelper.subscriptionToT2xNewspaperPackage") {
    val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelper/sub12/subscription.json")
    val today = LocalDate.of(2026, 9, 1)
    assertEquals(
      NewspaperHelper.subscriptionToT2xNewspaperPackage(subscription, today),
      Right(T2xEchoLegacy)
    )
  }

  // --------------------

  test("NewspaperHelper.subscriptionToDistribution") {
    // sub1: "Newspaper Voucher"          "Everyday+"   "GBP"   "Month"

    val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelper/sub1/subscription.json")
    val today = LocalDate.of(2026, 9, 1)

    // We are expecting: (NewspaperNationalDelivery, "Month", "Everyday+"), which comes down to
    /*
        List(
          T4xLeg(T1xMonday,      BigDecimal(11.4)),
          T4xLeg(T1xTuesday,     BigDecimal(11.4)),
          T4xLeg(T1xWednesday,   BigDecimal(11.4)),
          T4xLeg(T1xThursday,    BigDecimal(11.4)),
          T4xLeg(T1xFriday,      BigDecimal(11.4)),
          T4xLeg(T1xSaturday,    BigDecimal(14.7)),
          T4xLeg(T1xSunday,      BigDecimal(14.7)),
          T4xLeg(T1xDigitalPack, BigDecimal(13.6)),
        )
     */

    assertEquals(
      NewspaperHelper.subscriptionToFinancePercentageDistribution(subscription, today),
      Right(
        List(
          T4xLeg(T1xMonday, BigDecimal(11.4)),
          T4xLeg(T1xTuesday, BigDecimal(11.4)),
          T4xLeg(T1xWednesday, BigDecimal(11.4)),
          T4xLeg(T1xThursday, BigDecimal(11.4)),
          T4xLeg(T1xFriday, BigDecimal(11.4)),
          T4xLeg(T1xSaturday, BigDecimal(14.7)),
          T4xLeg(T1xSunday, BigDecimal(14.7)),
          T4xLeg(T1xDigitalPack, BigDecimal(13.6)),
        )
      )
    )
  }

  // --------------------

  test("NewspaperHelper.ratePlanChargeToMappingPair") {
    val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelper/sub1/subscription.json")
    val today = LocalDate.of(2026, 9, 1)
    val ratePlan = SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription, today).get
    val ratePlanCharge = ratePlan.ratePlanCharges.headOption.get

    assertEquals(
      NewspaperHelper.ratePlanChargeToMappingPair(ratePlanCharge),
      Right((T1xDigitalPack, "2c92a0fc56fe26ba01570418eddd26e1"))
    )
  }

  // --------------------

  test("NewspaperHelper.ratePlanChargeToMappingPair") {
    val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelper/sub1/subscription.json")
    val today = LocalDate.of(2026, 9, 1)
    val ratePlan = SI2025RateplanFromSub.uniquelyDeterminedActiveNonDiscountNonExpiredRatePlan(subscription, today).get

    // This is not a great test, originally I wanted to compare the two maps, but that doesn't quite work
    // So comparing two ids, will do.

    assertEquals(
      NewspaperHelper.ratePlanToProductRatePlanChargeIdMapping(ratePlan).get(T1xSunday),
      Some("2c92a0ff56fe33f5015709c80af30495")
    )
  }
}
