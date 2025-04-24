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


import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{EmploymentId, Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import v2.models.request.deleteAdditionalDirectorshipDividends.DeleteAdditionalDirectorshipDividendsRequest

import scala.concurrent.Future

class DeleteAdditionalDirectorshipDividendsConnectorSpec extends ConnectorSpec {

  private val nino: String = "AA123456A"

  private val employmentId: String = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"

  "DeleteAdditionalDirectorshipDividendsConnector" should {
    "return a 204 result on delete for a TYS request" when {
      "the downstream call is successful and tax year specific" in new HipTest with Test {
        def taxYear: TaxYear                               = TaxYear.fromMtd("2025-26")
        val outcome: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        willDelete(s"$baseUrl/itsd/income-sources/$nino/directorships/$employmentId?taxYear=${taxYear.asTysDownstream}").returns(Future.successful(outcome))

        val result: DownstreamOutcome[Unit] = await(connector.delete(request))
        result shouldBe outcome
      }
    }

  }

  trait Test {
    _: ConnectorTest =>

    def taxYear: TaxYear

    protected val connector: DeleteAdditionalDirectorshipDividendsConnector =
      new DeleteAdditionalDirectorshipDividendsConnector(
        http = mockHttpClient,
        appConfig = mockSharedAppConfig
      )


    protected val request: DeleteAdditionalDirectorshipDividendsRequest =
      DeleteAdditionalDirectorshipDividendsRequest(
        nino = Nino(nino),
        taxYear = taxYear,
        employmentId = EmploymentId(employmentId)
      )

  }

}
