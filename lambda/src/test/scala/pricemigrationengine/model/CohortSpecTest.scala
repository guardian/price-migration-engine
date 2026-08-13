package pricemigrationengine.model

import pricemigrationengine.model.CohortSpec.isValid
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import java.time.LocalDate
import scala.jdk.CollectionConverters._

class CohortSpecTest extends munit.FunSuite {

  private val cohortSpec = CohortSpec(
    cohortName = "HomeDelivery2018",
    active = true,
  )

  private def assertTrue(obtained: Boolean): Unit = assertEquals(obtained, true)
  private def assertFalse(obtained: Boolean): Unit = assertEquals(obtained, false)

  test("fromDynamoDbItem: should include all fields") {
    val item = Map(
      "cohortName" -> AttributeValue.builder.s("HomeDelivery2018").build(),
      "active" -> AttributeValue.builder.bool(true).build(),
    ).asJava
    assertEquals(
      CohortSpec.fromDynamoDbItem(item),
      Right(cohortSpec)
    )
  }

  test("isValid: should be true when all fields are valid") {
    assertTrue(isValid(cohortSpec))
  }

  test("isValid: should be false when the cohort name has trailing whitespace") {
    assertFalse(isValid(cohortSpec.copy(cohortName = "HomeDelivery 2018")))
  }
}
