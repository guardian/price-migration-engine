package pricemigrationengine.model

import java.time.LocalDate

import upickle.default._

import scala.io.Source

class BillingDateTest extends munit.FunSuite {

  private def currentDate = LocalDate.of(2020, 5, 4)
  private def migrationStartDate = LocalDate.of(2020, 12, 25)

  private def subscriptionFromJson(resource: String): ZuoraSubscription = {
    val subscriptionRaw = Source.fromResource(resource).mkString
    read[ZuoraSubscription](subscriptionRaw)
  }

  test("billing date is correct for a monthly discounted sub") {
    val sub = subscriptionFromJson("MonthlyDiscounted.json")
    val account = ZuoraAccount(ZuoraAccountBillingAndPayment(1))
    val startDate = BillingDate.nextBillingDate(sub, account, after = migrationStartDate, currentDate)
    assertEquals(startDate, Right(LocalDate.of(2021, 1, 8)))
  }
}
