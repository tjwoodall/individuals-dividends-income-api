/*
 * Copyright 2023 HM Revenue & Customs
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

package v1.controllers

import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.Result
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.domain.TaxYear
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import v1.mocks.services.MockRetrieveUkDividendsAnnualIncomeSummaryService
import v1.mocks.validators.MockRetrieveUkDividendsIncomeAnnualSummaryValidatorFactory
import v1.models.request.retrieveUkDividendsAnnualIncomeSummary.RetrieveUkDividendsIncomeAnnualSummaryRequest
import v1.models.response.retrieveUkDividendsAnnualIncomeSummary.RetrieveUkDividendsAnnualIncomeSummaryResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveUkDividendsIncomeAnnualSummaryControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockRetrieveUkDividendsAnnualIncomeSummaryService
    with MockRetrieveUkDividendsIncomeAnnualSummaryValidatorFactory {

  private val taxYear = "2019-20"

  private val requestData: RetrieveUkDividendsIncomeAnnualSummaryRequest = RetrieveUkDividendsIncomeAnnualSummaryRequest(
    nino = parsedNino,
    taxYear = TaxYear.fromMtd(taxYear)
  )

  private val retrieveUkDividendsAnnualIncomeSummaryResponseModel = RetrieveUkDividendsAnnualIncomeSummaryResponse(
    Some(100.99),
    Some(100.99)
  )

  private val mtdResponse = Json.parse("""
                                         |{
                                         |  "ukDividends":100.99,
                                         |  "otherUkDividends":100.99
                                         |}
                                         |""".stripMargin)

  "RetrieveDividendsController" should {
    "return a successful response with status 200 (OK)" when {
      "given a valid request" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveUkDividendsIncomeAnnualSummaryService
          .retrieveUkDividends(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveUkDividendsAnnualIncomeSummaryResponseModel))))

        runOkTest(
          expectedStatus = OK,
          maybeExpectedResponseBody = Some(mtdResponse)
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

        MockRetrieveUkDividendsIncomeAnnualSummaryService
          .retrieveUkDividends(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTest(RuleTaxYearNotSupportedError)
      }
    }
  }

  trait Test extends ControllerTest {

    val controller = new RetrieveUkDividendsAnnualIncomeSummaryController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockRetrieveUkDividendsAnnualIncomeSummaryValidatorFactory,
      service = mockRetrieveUkDividendsAnnualIncomeSummaryService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    protected def callController(): Future[Result] = controller.retrieveUkDividends(validNino, taxYear)(fakeGetRequest)

  }

}
