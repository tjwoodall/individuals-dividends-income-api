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
import shared.connectors.ConnectorSpec
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import v2.models.request.retrieveUkDividendsAnnualIncomeSummary.RetrieveUkDividendsIncomeAnnualSummaryRequest
import v2.models.response.retrieveUkDividendsAnnualIncomeSummary.RetrieveUkDividendsAnnualIncomeSummaryResponse
import uk.gov.hmrc.http.StringContextOps

import scala.concurrent.Future

class RetrieveUKDividendsIncomeAnnualSummaryConnectorSpec extends ConnectorSpec {

  val nino: String              = "AA111111A"
  val taxYearMtd: String        = "2018-19"
  val taxYearDownstream: String = "2019"
  val tysTaxYear: String        = "2023"

  private val validResponse = RetrieveUkDividendsAnnualIncomeSummaryResponse(
    ukDividends = Some(10.12),
    otherUkDividends = Some(11.12)
  )

  "RetrieveUkDividendsIncomeAnnualSummaryConnectorSpec and isDefIf_MigrationEnabled is off" when {
    "retrieveUKDividendsIncomeAnnualSummary is called" must {
      "return a 200 for success scenario" in {
        new DesTest with Test {
          MockedSharedAppConfig.featureSwitchConfig
            .once()
            .returns(
              Configuration(
                "isDesIfMigrationEnabled" -> "false"
              ))

          def taxYear: TaxYear = TaxYear.fromMtd("2018-19")

          val outcome = Right(ResponseWrapper(correlationId, validResponse))

          willGet(url"$baseUrl/income-tax/nino/$nino/income-source/dividends/annual/$taxYearDownstream")
            .returns(Future.successful(outcome))
        }
      }
    }

    "retrieveUKDividendsIncomeAnnualSummary is called and isDesIfMigrationEnabled is on" must {
      "return a 200 for success scenario" in {
        new IfsTest with Test {
          MockedSharedAppConfig.featureSwitchConfig
            .once()
            .returns(
              Configuration(
                "isDesIfMigrationEnabled" -> "true"
              ))

          def taxYear: TaxYear = TaxYear.fromMtd("2018-19")

          val outcome = Right(ResponseWrapper(correlationId, validResponse))

          willGet(url"$baseUrl/income-tax/nino/$nino/income-source/dividends/annual/$taxYearDownstream")
            .returns(Future.successful(outcome))

          await(connector.retrieveUKDividendsIncomeAnnualSummary(request)) shouldBe outcome

        }
      }
    }

    "retrieveUkDividendsIncomeAnnualSummary is called for a TaxYearSpecific tax year" must {
      "return a 200 for success scenario" in {
        new TysIfsTest with Test {
          def taxYear: TaxYear = TaxYear.fromMtd("2023-24")

          val outcome = Right(ResponseWrapper(correlationId, validResponse))

          willGet(url"$baseUrl/income-tax/${taxYear.asTysDownstream}/$nino/income-source/dividends/annual")
            .returns(Future.successful(outcome))

          await(connector.retrieveUKDividendsIncomeAnnualSummary(request)) shouldBe outcome
        }
      }
    }
  }

  trait Test { _: ConnectorTest =>
    def taxYear: TaxYear

    protected val connector: RetrieveUKDividendsIncomeAnnualSummaryConnector =
      new RetrieveUKDividendsIncomeAnnualSummaryConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)

    protected val request: RetrieveUkDividendsIncomeAnnualSummaryRequest =
      RetrieveUkDividendsIncomeAnnualSummaryRequest(Nino("AA111111A"), taxYear = taxYear)

  }

}
