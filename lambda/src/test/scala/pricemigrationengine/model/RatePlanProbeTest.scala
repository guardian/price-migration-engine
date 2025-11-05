package pricemigrationengine.migrations

import pricemigrationengine.model._

import java.time.LocalDate
import pricemigrationengine.Fixtures
import pricemigrationengine.model.RatePlanProbe

class RatePlanProbeTest extends munit.FunSuite {

  test("ratePlanDate is correct") {
    val subscription =
      Fixtures.subscriptionFromJson("model/RatePlanProbe/NewspaperHomeDelivery-Quarterly/subscription.json")
    val ratePlan = subscription.ratePlans.headOption.get // we get the first rate plan
    assertEquals(RatePlanProbe.ratePlanDate(ratePlan: ZuoraRatePlan), Some(LocalDate.of(2023, 10, 19)))
  }

  test("addedRatePlansAfterDate is correct") {
    val subscription =
      Fixtures.subscriptionFromJson("model/RatePlanProbe/NewspaperHomeDelivery-Quarterly/subscription.json")

    // The subscription has 41 subscriptions but only 39 have the "lastChangeType" set to "Add";
    // one is set to "Remove" and the last one is not set.
    // They are all after 2016. There are 6 in 2023 and none in 2024
    assertEquals(
      RatePlanProbe.addedRatePlansAfterDate(subscription: ZuoraSubscription, LocalDate.of(2016, 1, 1)).size,
      39
    )
    assertEquals(
      RatePlanProbe.addedRatePlansAfterDate(subscription: ZuoraSubscription, LocalDate.of(2023, 1, 1)).size,
      6
    )
    assertEquals(
      RatePlanProbe.addedRatePlansAfterDate(subscription: ZuoraSubscription, LocalDate.of(2024, 1, 1)).size,
      0
    )
  }

  test("selectNonTrivialRatePlans is correct") {
    val subscription =
      Fixtures.subscriptionFromJson("model/RatePlanProbe/NewspaperHomeDelivery-Quarterly/subscription.json")

    // There is one active non trivial rate plan if we count since inception

    val ratePlans = RatePlanProbe.addedRatePlansAfterDate(subscription: ZuoraSubscription, LocalDate.of(2016, 1, 1))
    val nonTrivialRatePlans = RatePlanProbe.selectNonTrivialRatePlans(ratePlans)
    assertEquals(
      nonTrivialRatePlans.size,
      1
    )

    // That the current "Newspaper Delivery"

    val nonTrivialRatePlan = nonTrivialRatePlans.headOption.get
    assertEquals(
      nonTrivialRatePlan.productName,
      "Newspaper Delivery"
    )

    // It was subscribed to in 2022, so it should not show up if we query for rate plan after "2024-01-01"

    assertEquals(
      RatePlanProbe
        .selectNonTrivialRatePlans(
          RatePlanProbe.addedRatePlansAfterDate(subscription: ZuoraSubscription, LocalDate.of(2024, 1, 1))
        )
        .size,
      0
    )
  }

  test("probe is correct") {
    val subscription =
      Fixtures.subscriptionFromJson("model/RatePlanProbe/NewspaperHomeDelivery-Quarterly-Cancelled/subscription.json")
    assertEquals(
      RatePlanProbe.probe(subscription, LocalDate.of(2024, 1, 1)),
      RPPCancelledInZuora
    )
  }

}
