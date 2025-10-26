package pricemigrationengine.handlers

import pricemigrationengine.model._

class NotificationHandlerHelperTest extends munit.FunSuite {
  test("assertNonTrivialValue") {
    assertEquals(NotificationHandlerHelper.assertNonTrivialValue("value name", None), false)
    assertEquals(NotificationHandlerHelper.assertNonTrivialValue("value name", Some("")), false)
    assertEquals(NotificationHandlerHelper.assertNonTrivialValue("value name", Some("thing")), true)
  }
}
