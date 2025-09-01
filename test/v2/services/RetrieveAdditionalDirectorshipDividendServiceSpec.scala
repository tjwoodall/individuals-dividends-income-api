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

package v2.services

import shared.models.domain._
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.{ServiceOutcome, ServiceSpec}
import v2.fixtures.RetrieveAdditionalDirectorshipDividendFixtures._
import v2.mocks.connectors.MockRetrieveAdditionalDirectorshipDividendConnector
import v2.models.request.retrieveAdditionalDirectorshipDividend.RetrieveAdditionalDirectorshipDividendRequest
import v2.models.response.retrieveAdditionalDirectorshipDividend.RetrieveAdditionalDirectorshipDividendResponse

import scala.concurrent.Future

class RetrieveAdditionalDirectorshipDividendServiceSpec extends ServiceSpec {

  private val nino: Nino                 = Nino("AA123456A")
  private val taxYear: TaxYear           = TaxYear.fromMtd("2025-26")
  private val employmentId: EmploymentId = EmploymentId("4557ecb5-fd32-48cc-81f5-e6acd1099f3c")

  "RetrieveAdditionalDirectorshipDividendService" when {
    "retrieve" should {
      "return correct result for a success" in new Test {
        val outcome: Right[Nothing, ResponseWrapper[RetrieveAdditionalDirectorshipDividendResponse]] =
          Right(ResponseWrapper(correlationId, responseModel))

        MockRetrieveAdditionalDirectorshipDividendConnector
          .retrieve(request)
          .returns(Future.successful(outcome))

        val result: ServiceOutcome[RetrieveAdditionalDirectorshipDividendResponse] = await(service.retrieve(request))

        result shouldBe outcome
      }

      "map errors according to spec" when {
        def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
          s"a $downstreamErrorCode error is returned from the service" in new Test {

            MockRetrieveAdditionalDirectorshipDividendConnector
              .retrieve(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            val result: ServiceOutcome[RetrieveAdditionalDirectorshipDividendResponse] = await(service.retrieve(request))

            result shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val errors = List(
          ("1215", NinoFormatError),
          ("1117", TaxYearFormatError),
          ("1217", EmploymentIdFormatError),
          ("1216", InternalError),
          ("5010", NotFoundError)
        )

        errors.foreach(args => serviceError.tupled(args))
      }
    }
  }

  trait Test extends MockRetrieveAdditionalDirectorshipDividendConnector {

    val request: RetrieveAdditionalDirectorshipDividendRequest = RetrieveAdditionalDirectorshipDividendRequest(
      nino = nino,
      taxYear = taxYear,
      employmentId = employmentId
    )

    val service: RetrieveAdditionalDirectorshipDividendService = new RetrieveAdditionalDirectorshipDividendService(
      connector = mockRetrieveAdditionalDirectorshipDividendConnector
    )

  }

}
