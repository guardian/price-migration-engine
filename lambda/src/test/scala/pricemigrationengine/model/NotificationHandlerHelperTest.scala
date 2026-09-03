package pricemigrationengine.handlers

import pricemigrationengine.model._

class NotificationHandlerHelperTest extends munit.FunSuite {
  test("basic scala used in assertNonTrivialValue") {
    assertEquals("".isEmpty, true)
    assertEquals("thing".isEmpty, false)
  }
  test("basic scala used in messageIsWellFormed") {
    assertEquals(List(true, false).forall(identity), false)
    assertEquals(List(true, true).forall(identity), true)
  }
  test("assertNonTrivialValue") {
    assertEquals(NotificationHandlerHelper.assertNonTrivialValue(None), false)
    assertEquals(NotificationHandlerHelper.assertNonTrivialValue(Some("")), false)
    assertEquals(NotificationHandlerHelper.assertNonTrivialValue(Some("thing")), true)
  }
}
