package pricemigrationengine.model

import pricemigrationengine.Fixtures

import java.time.LocalDate

// Newspaper:
// sub1: "Newspaper Voucher"          "Everyday+"   "GBP"   "Month"
// val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelperTest/sub1/subscription.json")
// val account = Fixtures.accountFromJson("model/NewspaperHelperTest/sub1/account.json")
// val invoicePreview = Fixtures.invoiceListFromJson("model/NewspaperHelperTest/sub1/invoice-preview.json")

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
  test("NewspaperHelper.subscriptionToT3xDeliveryCategory") {
    val subscription = Fixtures.subscriptionFromJson("model/NewspaperHelperTest/sub1/subscription.json")
    val today = LocalDate.of(2026, 9, 22)
    assertEquals(
      NewspaperHelper.subscriptionToT3xDeliveryCategory(subscription, today),
      Some(T3xNewspaperVoucher)
    )
  }
}
