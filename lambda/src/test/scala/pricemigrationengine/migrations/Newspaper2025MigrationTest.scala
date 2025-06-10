package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate

class Newspaper2025MigrationTest extends munit.FunSuite {

  test("priceLookUp") {
    assertEquals(
      Newspaper2025Migration.priceLookUp(Voucher, EverydayPlus, Monthly),
      Some(BigDecimal(69.99))
    )

    assertEquals(
      Newspaper2025Migration.priceLookUp(Voucher, SixdayPlus, SemiAnnual),
      Some(BigDecimal(371.94))
    )

    assertEquals(
      Newspaper2025Migration.priceLookUp(Subcard, EverydayPlus, Quarterly),
      Some(BigDecimal(209.97))
    )

    assertEquals(
      Newspaper2025Migration.priceLookUp(HomeDelivery, SixdayPlus, Monthly),
      Some(BigDecimal(73.99))
    )

    // And we test an undefined combination
    assertEquals(
      Newspaper2025Migration.priceLookUp(HomeDelivery, SixdayPlus, SemiAnnual),
      None
    )
  }
}
