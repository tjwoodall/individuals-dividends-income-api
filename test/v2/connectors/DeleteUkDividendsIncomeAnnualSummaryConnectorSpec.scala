/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v2.connectors

import play.api.Configuration
import play.api.libs.json.Json
import shared.connectors.ConnectorSpec
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import v2.models.request.deleteUkDividendsIncomeAnnualSummary.DeleteUkDividendsIncomeAnnualSummaryRequest
import uk.gov.hmrc.http.StringContextOps

import scala.concurrent.Future

class DeleteUkDividendsIncomeAnnualSummaryConnectorSpec extends ConnectorSpec {

  "DeleteUkDividendsIncomeAnnualSummaryConnector" should {
    "return the expected response for a non-TYS request" when {
      "a valid request is made and `isPassDeleteIntentEnabled` feature switch is on and isDesIfMigrationEnabled is on" in new IfsTest with Test {
        def taxYear: TaxYear = TaxYear.fromMtd("2019-20")
        MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration(
            "desIf_Migration.enabled" -> "true"
          ))

        val outcome = Right(ResponseWrapper(correlationId, ()))

        willPost(
          url = url"$baseUrl/income-tax/nino/$nino/income-source/dividends/annual/${taxYear.asDownstream}",
          body = Json.parse("""{}""")
        ).returns(Future.successful(outcome))

        MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration(
          "passDeleteIntentHeader.enabled" -> "true"
        ))

        await(connector.delete(request)) shouldBe outcome
      }

      "a valid request is made and `isPassDeleteIntentEnabled` feature switch is on and isDefIfMigrationEnabled is off" in new DesTest with Test {
        MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration(
            "desIf_Migration.enabled" -> "false"
          ))

        def taxYear: TaxYear = TaxYear.fromMtd("2019-20")
        val outcome          = Right(ResponseWrapper(correlationId, ()))

        willPost(
          url = url"$baseUrl/income-tax/nino/$nino/income-source/dividends/annual/${taxYear.asDownstream}",
          body = Json.parse("""{}""")
        ).returns(Future.successful(outcome))

        MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration(
          "passDeleteIntentHeader.enabled" -> "true"
        ))

        await(connector.delete(request)) shouldBe outcome
      }

      "a valid request is made and `isPassDeleteIntentEnabled` feature switch is off and isDesMigrationEnabled is off" in new DesTest with Test {
        MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration(
            "desIf_Migration.enabled" -> "false"
          ))

        def taxYear: TaxYear = TaxYear.fromMtd("2019-20")
        val outcome          = Right(ResponseWrapper(correlationId, ()))

        willPost(
          url = url"$baseUrl/income-tax/nino/$nino/income-source/dividends/annual/${taxYear.asDownstream}",
          body = Json.parse("""{}""")
        ).returns(Future.successful(outcome))

        MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration(
          "passDeleteIntentHeader.enabled" -> "false"
        ))

        await(connector.delete(request)) shouldBe outcome
      }

      "a valid request is made and `isPassDeleteIntentEnabled` feature switch is off and isDesMigrationEnabled is on" in new IfsTest with Test {
        def taxYear: TaxYear = TaxYear.fromMtd("2019-20")
        MockedSharedAppConfig.featureSwitchConfig.once().returns(Configuration(
            "desIf_Migration.enabled" -> "true"
          ))

        val outcome = Right(ResponseWrapper(correlationId, ()))

        willPost(
          url = url"$baseUrl/income-tax/nino/$nino/income-source/dividends/annual/${taxYear.asDownstream}",
          body = Json.parse("""{}""")
        ).returns(Future.successful(outcome))

        MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration(
          "passDeleteIntentHeader.enabled" -> "false"
        ))

        await(connector.delete(request)) shouldBe outcome
      }
    }
    "return the expected response for a TYS request" when {
      "a valid request is made" in new TysIfsTest with Test {
        def taxYear: TaxYear = TaxYear.fromMtd("2023-24")
        val outcome          = Right(ResponseWrapper(correlationId, ()))

        willDelete(url = url"$baseUrl/income-tax/${taxYear.asTysDownstream}/$nino/income-source/dividends/annual")
          .returns(Future.successful(outcome))

        MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration(
          "passDeleteIntentHeader.enabled" -> "false"
        ))

        await(connector.delete(request)) shouldBe outcome
      }
    }
  }

  trait Test { _: ConnectorTest =>

    def taxYear: TaxYear
    val nino: String = "AA123456A"

    val request: DeleteUkDividendsIncomeAnnualSummaryRequest = DeleteUkDividendsIncomeAnnualSummaryRequest(Nino(nino), taxYear)

    val connector: DeleteUkDividendsIncomeAnnualSummaryConnector = new DeleteUkDividendsIncomeAnnualSummaryConnector(
      http = mockHttpClient,
      appConfig = mockSharedAppConfig
    )

  }

}
