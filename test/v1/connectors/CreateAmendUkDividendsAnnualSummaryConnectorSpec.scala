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

package v1.connectors

import play.api.Configuration
import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import v1.models.request.createAmendUkDividendsIncomeAnnualSummary.{
  CreateAmendUkDividendsIncomeAnnualSummaryBody,
  CreateAmendUkDividendsIncomeAnnualSummaryRequest
}

import scala.concurrent.Future

class CreateAmendUkDividendsAnnualSummaryConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"
  private val body = CreateAmendUkDividendsIncomeAnnualSummaryBody(None, None)

  "CreateAmendUkDividendsAnnualSummaryConnector" when {
    "createOrAmendAnnualSummary called and 'isDesIfMigrationEnabled' is off" must {
      "return a 200 status for a success scenario" in
        new DesTest with Test {
          MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration(
            "desIf_Migration.enabled" -> "false"
          ))

          def taxYear: TaxYear = TaxYear.fromMtd("2019-20")

          val outcome = Right(ResponseWrapper(correlationId, ()))

          willPost(s"$baseUrl/income-tax/nino/$nino/income-source/dividends/annual/${taxYear.asDownstream}", body) returns Future.successful(outcome)

          val result: DownstreamOutcome[Unit] = await(connector.createAmendUkDividends(request))
          result shouldBe outcome
        }
    }

    "createOrAmendAnnualSummary called and 'isDesIfMigrationEnabled' is on" must {
      "return a 200 status for a success scenario" in
        new IfsTest with Test {
          MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration(
            "desIf_Migration.enabled" -> "true"
          ))

          def taxYear: TaxYear = TaxYear.fromMtd("2019-20")

          val outcome = Right(ResponseWrapper(correlationId, ()))

          willPost(s"$baseUrl/income-tax/nino/$nino/income-source/dividends/annual/${taxYear.asDownstream}", body) returns Future.successful(outcome)

          val result: DownstreamOutcome[Unit] = await(connector.createAmendUkDividends(request))
          result shouldBe outcome
        }
    }

    "createOrAmendAnnualSummary called for a Tax Year Specific tax year" must {
      "return a 200 status for a success scenario" in
        new TysIfsTest with Test {
          def taxYear: TaxYear = TaxYear.fromMtd("2023-24")

          val outcome = Right(ResponseWrapper(correlationId, ()))

          willPost(s"$baseUrl/income-tax/${taxYear.asTysDownstream}/$nino/income-source/dividends/annual", body) returns Future.successful(outcome)

          val result: DownstreamOutcome[Unit] = await(connector.createAmendUkDividends(request))
          result shouldBe outcome
        }
    }
  }

  trait Test { _: ConnectorTest =>
    def taxYear: TaxYear

    protected val connector: CreateAmendUkDividendsAnnualSummaryConnector =
      new CreateAmendUkDividendsAnnualSummaryConnector(
        http = mockHttpClient,
        appConfig = mockSharedAppConfig
      )

    protected val request: CreateAmendUkDividendsIncomeAnnualSummaryRequest =
      CreateAmendUkDividendsIncomeAnnualSummaryRequest(
        nino = Nino(nino),
        taxYear = taxYear,
        body = body
      )

  }

}
