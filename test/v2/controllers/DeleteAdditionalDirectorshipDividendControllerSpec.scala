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
import play.api.libs.json.JsValue
import play.api.mvc.Result
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import shared.models.domain.{EmploymentId, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import v2.mocks.services.MockDeleteAdditionalDirectorshipDividendService
import v2.mocks.validators.MockDeleteAdditionalDirectorshipDividendValidatorFactory
import v2.models.request.deleteAdditionalDirectorshipDividend.DeleteAdditionalDirectorshipDividendRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteAdditionalDirectorshipDividendControllerSpec
  extends ControllerBaseSpec
    with ControllerTestRunner
    with MockDeleteAdditionalDirectorshipDividendService
    with MockDeleteAdditionalDirectorshipDividendValidatorFactory {

  private val taxYear: String      = "2025-26"
  private val employmentId: String = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"

  private val requestData: DeleteAdditionalDirectorshipDividendRequest = DeleteAdditionalDirectorshipDividendRequest(
    nino = parsedNino,
    taxYear = TaxYear.fromMtd(taxYear),
    employmentId = EmploymentId(employmentId)
  )

  "DeleteAdditionalDirectorshipDividendController" should {
    "return a successful response with status 204 (NO_CONTENT)" when {
      "given a valid request" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteAdditionalDirectorshipDividendService
          .delete(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(expectedStatus = NO_CONTENT)
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTestWithAudit(NinoFormatError)
      }

      "service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteAdditionalDirectorshipDividendService
          .delete(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, EmploymentIdFormatError))))

        runErrorTestWithAudit(EmploymentIdFormatError)
      }
    }
  }

  trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller: DeleteAdditionalDirectorshipDividendController = new DeleteAdditionalDirectorshipDividendController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockDeleteAdditionalDirectorshipDividendValidatorFactory,
      service = mockDeleteAdditionalDirectorshipDividendService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    protected def callController(): Future[Result] = controller.delete(validNino, taxYear, employmentId)(fakeRequest)

    def event(auditResponse: AuditResponse, requestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "DeleteAdditionalDirectorshipDividend",
        transactionName = "delete-additional-directorship-dividend",
        detail = GenericAuditDetail(
          userType = "Individual",
          versionNumber = apiVersion.name,
          agentReferenceNumber = None,
          params = Map("nino" -> validNino, "taxYear" -> taxYear, "employmentId" -> employmentId),
          `X-CorrelationId` = correlationId,
          requestBody = None,
          auditResponse = auditResponse
        )
      )

  }

}
