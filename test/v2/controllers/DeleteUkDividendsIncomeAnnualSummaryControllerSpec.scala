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

import api.config.MockAppConfig
import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.models.audit.{AuditEvent, AuditResponse, FlattenedGenericAuditDetail}
import api.models.auth.UserDetails
import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc.Result
import v2.mocks.services.MockDeleteUkDividendsIncomeAnnualSummaryService
import v2.mocks.validators.MockDeleteUkDividendsIncomeAnnualSummaryValidatorFactory
import v2.models.request.deleteUkDividendsIncomeAnnualSummary.DeleteUkDividendsIncomeAnnualSummaryRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteUkDividendsIncomeAnnualSummaryControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockDeleteUkDividendsIncomeAnnualSummaryService
    with MockDeleteUkDividendsIncomeAnnualSummaryValidatorFactory
    with MockAppConfig {

  private val taxYear = "2017-18"
  private val mtdId   = "test-mtd-id"

  private val requestData: DeleteUkDividendsIncomeAnnualSummaryRequest = DeleteUkDividendsIncomeAnnualSummaryRequest(
    nino = Nino(validNino),
    taxYear = TaxYear.fromMtd(taxYear)
  )

  "DeleteDividendsController" should {
    "return a successful response with status 204 (No Content)" when {
      "a valid request is supplied" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteUkDividendsIncomeAnnualSummaryService
          .delete(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTest(expectedStatus = NO_CONTENT)
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTest(NinoFormatError)
      }

      "service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteUkDividendsIncomeAnnualSummaryService
          .delete(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTest(RuleTaxYearNotSupportedError)
      }
    }
  }

  trait Test extends ControllerTest with AuditEventChecking[FlattenedGenericAuditDetail] {

    val controller: DeleteUkDividendsIncomeAnnualSummaryController = new DeleteUkDividendsIncomeAnnualSummaryController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockDeleteUkDividendsIncomeAnnualSummaryValidatorFactory,
      service = mockDeleteUkDividendsIncomeAnnualSummaryService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    protected def callController(): Future[Result] = controller.deleteUkDividends(validNino, taxYear)(fakeRequest)

    def event(auditResponse: AuditResponse, requestBody: Option[JsValue]): AuditEvent[FlattenedGenericAuditDetail] =
      AuditEvent(
        auditType = "DeleteUkDividendsIncome",
        transactionName = "delete-uk-dividends-income",
        detail = FlattenedGenericAuditDetail(
          userDetails = UserDetails(mtdId, "Individual", None),
          versionNumber = Some("2.0"),
          params = Map("nino" -> validNino, "taxYear" -> taxYear),
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse,
          futureYears = None,
          history = None,
          itsaStatuses = None
        )
      )

  }

}
