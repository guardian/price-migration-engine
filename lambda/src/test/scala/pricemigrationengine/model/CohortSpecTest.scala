package pricemigrationengine.model

import pricemigrationengine.model.CohortSpec.isValid
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import java.time.LocalDate
import scala.jdk.CollectionConverters._

class CohortSpecTest extends munit.FunSuite {

  private val cohortSpec = CohortSpec(
    cohortName = "Home Delivery 2018",
    brazeName = "cmp123",
    earliestPriceMigrationStartDate = LocalDate.of(2020, 1, 2)
  )

  private def assertTrue(obtained: Boolean): Unit = assertEquals(obtained, true)
  private def assertFalse(obtained: Boolean): Unit = assertEquals(obtained, false)

  test("fromDynamoDbItem: should include all fields") {
    val item = Map(
      "cohortName" -> AttributeValue.builder.s("Home Delivery 2018").build(),
      "brazeName" -> AttributeValue.builder.s("cmp123").build(),
      "earliestPriceMigrationStartDate" -> AttributeValue.builder.s("2020-01-02").build()
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
    assertFalse(isValid(cohortSpec.copy(cohortName = "Home Delivery 2018 ")))
  }

  test("isValid: should be false when the campaign name contains an illegal character") {
    assertFalse(isValid(cohortSpec.copy(brazeName = "vc:ppr321")))
  }
}
