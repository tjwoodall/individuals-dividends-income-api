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
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.Result
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.audit.{AuditEvent, AuditResponse, FlattenedGenericAuditDetail}
import shared.models.auth.UserDetails
import shared.models.domain.TaxYear
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import v2.mocks.services.MockCreateAmendUkDividendsAnnualSummaryService
import v2.mocks.validators.MockCreateAmendUkDividendsAnnualSummaryValidatorFactory
import v2.models.request.createAmendUkDividendsIncomeAnnualSummary._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateAmendUkDividendsAnnualSummaryControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockCreateAmendUkDividendsAnnualSummaryService
    with MockCreateAmendUkDividendsAnnualSummaryValidatorFactory
    with MockSharedAppConfig {

  private val taxYear = "2019-20"
  private val mtdId   = "test-mtd-id"

  private val requestJson: JsObject = JsObject.empty

  private val requestModel: CreateAmendUkDividendsIncomeAnnualSummaryBody = CreateAmendUkDividendsIncomeAnnualSummaryBody(None, None)

  private val requestData: CreateAmendUkDividendsIncomeAnnualSummaryRequest = CreateAmendUkDividendsIncomeAnnualSummaryRequest(
    nino = parsedNino,
    taxYear = TaxYear.fromMtd(taxYear),
    body = requestModel
  )

  "CreateAmendUkDividendsAnnualSummaryController" should {
    "return a successful response with status 200 (OK)" when {
      "given a valid request" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockCreateAmendAmendUkDividendsAnnualSummaryService
          .createOrAmendAnnualSummary(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = None)

      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockCreateAmendAmendUkDividendsAnnualSummaryService
          .createOrAmendAnnualSummary(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTest(RuleTaxYearNotSupportedError)
      }
    }
  }

  trait Test extends ControllerTest with AuditEventChecking[FlattenedGenericAuditDetail] {

    val controller: CreateAmendUkDividendsAnnualSummaryController = new CreateAmendUkDividendsAnnualSummaryController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockCreateAmendUkDividendsAnnualSummaryValidatorFactory,
      service = mockCreateAmendUkDividendsAnnualSummaryService,
      cc = cc,
      idGenerator = mockIdGenerator,
      auditService = mockAuditService
    )

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    protected def callController(): Future[Result] =
      controller.createAmendUkDividendsAnnualSummary(validNino, taxYear)(fakeRequest.withBody(requestJson))

    def event(auditResponse: AuditResponse, requestBody: Option[JsValue]): AuditEvent[FlattenedGenericAuditDetail] = {
      AuditEvent(
        auditType = "CreateAndAmendUkDividendsIncome",
        transactionName = "create-amend-uk-dividends-income",
        detail = FlattenedGenericAuditDetail(
          versionNumber = Some("2.0"),
          userDetails = UserDetails(mtdId, "Individual", None),
          params = Map("nino" -> validNino, "taxYear" -> taxYear),
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse,
          history = None,
          futureYears = None,
          itsaStatuses = None
        )
      )
    }

  }

}
