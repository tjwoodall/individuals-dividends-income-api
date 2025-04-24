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

import shared.models.domain.{EmploymentId, Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v2.mocks.connectors.MockDeleteAdditionalDirectorshipDividendsConnector
import v2.models.request.deleteAdditionalDirectorshipDividends.DeleteAdditionalDirectorshipDividendsRequest

import scala.concurrent.Future

class DeleteAdditionalDirectorshipDividendsServiceSpec extends ServiceSpec {

  private val nino    = "AA112233A"
  private val taxYear = "2025-26"
  private val employmentId = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"

  trait Test extends MockDeleteAdditionalDirectorshipDividendsConnector {

    val request: DeleteAdditionalDirectorshipDividendsRequest = DeleteAdditionalDirectorshipDividendsRequest(Nino(nino), TaxYear.fromMtd(taxYear), EmploymentId(employmentId))

    val service: DeleteAdditionalDirectorshipDividendsService = new DeleteAdditionalDirectorshipDividendsService(
      connector = mockDeleteAdditionalDirectorshipDividendsConnector
    )

  }

  "DeleteAdditionalDirectorshipDividendsService" when {
    "delete" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockDeleteAdditionalDirectorshipDividendsConnector
          .delete(request)
          .returns(Future.successful(outcome))

        val result: Either[ErrorWrapper, ResponseWrapper[Unit]] = await(service.delete(request))
        result shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
          s"a $downstreamErrorCode error is returned from the service" in new Test {

            MockDeleteAdditionalDirectorshipDividendsConnector
              .delete(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            val result: Either[ErrorWrapper, ResponseWrapper[Unit]] = await(service.delete(request))
            result shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val errors = Map(
          "1215" -> NinoFormatError,
          "1117" -> TaxYearFormatError,
          "1217" -> EmploymentIdFormatError,
          "5010" -> NotFoundError,
          "1216" -> InternalError,
        )
        errors.foreach(args => (serviceError _).tupled(args))
      }
    }

  }

}
