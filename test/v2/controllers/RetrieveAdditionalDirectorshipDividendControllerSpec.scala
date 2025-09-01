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

package v2.controllers

import play.api.Configuration
import play.api.mvc.Result
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.domain.{EmploymentId, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import v2.fixtures.RetrieveAdditionalDirectorshipDividendFixtures._
import v2.mocks.services.MockRetrieveAdditionalDirectorshipDividendService
import v2.mocks.validators.MockRetrieveAdditionalDirectorshipDividendValidatorFactory
import v2.models.request.retrieveAdditionalDirectorshipDividend.RetrieveAdditionalDirectorshipDividendRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveAdditionalDirectorshipDividendControllerSpec extends ControllerBaseSpec with ControllerTestRunner {

  private val taxYear: String      = "2025-26"
  private val employmentId: String = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"

  private val requestData: RetrieveAdditionalDirectorshipDividendRequest = RetrieveAdditionalDirectorshipDividendRequest(
    nino = parsedNino,
    taxYear = TaxYear.fromMtd(taxYear),
    employmentId = EmploymentId(employmentId)
  )

  "RetrieveAdditionalDirectorshipDividendController" should {
    "return a successful response with status 200 (OK)" when {
      "given a valid request" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveAdditionalDirectorshipDividendService
          .retrieve(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseModel))))

        runOkTest(
          expectedStatus = OK,
          maybeExpectedResponseBody = Some(responseJson)
        )
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveAdditionalDirectorshipDividendService
          .retrieve(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, EmploymentIdFormatError))))

        runErrorTest(EmploymentIdFormatError)
      }
    }
  }

  trait Test
      extends ControllerTest
      with MockRetrieveAdditionalDirectorshipDividendService
      with MockRetrieveAdditionalDirectorshipDividendValidatorFactory {

    val controller: RetrieveAdditionalDirectorshipDividendController = new RetrieveAdditionalDirectorshipDividendController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockRetrieveAdditionalDirectorshipDividendValidatorFactory,
      service = mockRetrieveAdditionalDirectorshipDividendService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    protected def callController(): Future[Result] = controller.retrieve(validNino, taxYear, employmentId)(fakeGetRequest)

  }

}
