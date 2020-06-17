package pricemigrationengine.handlers

import java.time.LocalDate

import pricemigrationengine.Fixtures
import pricemigrationengine.model.{AmendmentConfig, ConfigurationFailure}
import pricemigrationengine.services.AmendmentConfiguration
import zio._
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation._
import zio.test.mock.{Mock, MockRandom, Proxy}

object EstimationHandlerSpec extends DefaultRunnableSpec {

  private object AmendConfigurationMock extends Mock[AmendmentConfiguration] {

    object Config extends Effect[Unit, ConfigurationFailure, AmendmentConfig]

    val compose: URLayer[Has[Proxy], AmendmentConfiguration] = ZLayer.fromServiceM { proxy =>
      withRuntime.as(new AmendmentConfiguration.Service {
        val config: IO[ConfigurationFailure, AmendmentConfig] = proxy(Config)
      })
    }
  }

  private val env = {
    val config = AmendmentConfig(LocalDate.of(2020, 6, 2))
    AmendConfigurationMock.Config(value(config)) andThen
      MockRandom.NextIntBetween(equalTo((0, 3)), value(1))
  }

  def spec = suite("EstimationHandler") {
    suite("spreadEarliestStartDate")(
      testM("gives randomised value for a monthly subscription") {
        val (subscription, invoicePreview) = Fixtures.subscriptionAndInvoicePreviewFromFolder("Monthly")
        val earliestStartDate =
          EstimationHandler.spreadEarliestStartDate(subscription, invoicePreview).provideLayer(env)
        assertM(earliestStartDate)(equalTo(LocalDate.of(2020, 7, 2)))
      }
    )
  }
}
