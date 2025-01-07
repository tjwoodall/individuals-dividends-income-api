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

package v1.controllers

import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import shared.models.domain.TaxYear
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import shared.utils.MockIdGenerator
import v1.mocks.services.MockCreateAmendDividendsService
import v1.mocks.validators.MockCreateAmendDividendsValidatorFactory
import v1.models.request.createAmendDividends._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateAmendDividendsControllerSpec
  extends ControllerBaseSpec
    with ControllerTestRunner
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockSharedAppConfig
    with MockAuditService
    with MockCreateAmendDividendsService
    with MockCreateAmendDividendsValidatorFactory
    with MockIdGenerator {

  val taxYear: String = "2019-20"

  val validRequestJson: JsValue = Json.parse(
    """
      |{
      |   "foreignDividend": [
      |      {
      |        "countryCode": "DEU",
      |        "amountBeforeTax": 1232.22,
      |        "taxTakenOff": 22.22,
      |        "specialWithholdingTax": 27.35,
      |        "foreignTaxCreditRelief": true,
      |        "taxableAmount": 2321.22
      |      },
      |      {
      |        "countryCode": "FRA",
      |        "amountBeforeTax": 1350.55,
      |        "taxTakenOff": 25.27,
      |        "specialWithholdingTax": 30.59,
      |        "foreignTaxCreditRelief": false,
      |        "taxableAmount": 2500.99
      |      }
      |   ],
      |   "dividendIncomeReceivedWhilstAbroad": [
      |      {
      |        "countryCode": "DEU",
      |        "amountBeforeTax": 1232.22,
      |        "taxTakenOff": 22.22,
      |        "specialWithholdingTax": 27.35,
      |        "foreignTaxCreditRelief": true,
      |        "taxableAmount": 2321.22
      |      },
      |      {
      |        "countryCode": "FRA",
      |        "amountBeforeTax": 1350.55,
      |        "taxTakenOff": 25.27,
      |        "specialWithholdingTax": 30.59,
      |        "foreignTaxCreditRelief": false,
      |        "taxableAmount": 2500.99
      |       }
      |   ],
      |   "stockDividend": {
      |      "customerReference": "my divs",
      |      "grossAmount": 12321.22
      |   },
      |   "redeemableShares": {
      |      "customerReference": "my shares",
      |      "grossAmount": 12345.75
      |   },
      |   "bonusIssuesOfSecurities": {
      |      "customerReference": "my secs",
      |      "grossAmount": 12500.89
      |   },
      |   "closeCompanyLoansWrittenOff": {
      |      "customerReference": "write off",
      |      "grossAmount": 13700.55
      |   }
      |}
    """.stripMargin
  )

  val foreignDividend: List[CreateAmendForeignDividendItem] = List(
    CreateAmendForeignDividendItem(
      countryCode = "DEU",
      amountBeforeTax = Some(1232.22),
      taxTakenOff = Some(22.22),
      specialWithholdingTax = Some(27.35),
      foreignTaxCreditRelief = Some(true),
      taxableAmount = 2321.22
    ),
    CreateAmendForeignDividendItem(
      countryCode = "FRA",
      amountBeforeTax = Some(1350.55),
      taxTakenOff = Some(25.27),
      specialWithholdingTax = Some(30.59),
      foreignTaxCreditRelief = Some(false),
      taxableAmount = 2500.99
    )
  )

  val dividendIncomeReceivedWhilstAbroad: List[CreateAmendDividendIncomeReceivedWhilstAbroadItem] = List(
    CreateAmendDividendIncomeReceivedWhilstAbroadItem(
      countryCode = "DEU",
      amountBeforeTax = Some(1232.22),
      taxTakenOff = Some(22.22),
      specialWithholdingTax = Some(27.35),
      foreignTaxCreditRelief = Some(true),
      taxableAmount = 2321.22
    ),
    CreateAmendDividendIncomeReceivedWhilstAbroadItem(
      countryCode = "FRA",
      amountBeforeTax = Some(1350.55),
      taxTakenOff = Some(25.27),
      specialWithholdingTax = Some(30.59),
      foreignTaxCreditRelief = Some(false),
      taxableAmount = 2500.99
    )
  )

  val stockDividend: CreateAmendCommonDividends = CreateAmendCommonDividends(
    customerReference = Some("my divs"),
    grossAmount = 12321.22
  )

  val redeemableShares: CreateAmendCommonDividends = CreateAmendCommonDividends(
    customerReference = Some("my shares"),
    grossAmount = 12345.75
  )

  val bonusIssuesOfSecurities: CreateAmendCommonDividends = CreateAmendCommonDividends(
    customerReference = Some("my secs"),
    grossAmount = 12500.89
  )

  val closeCompanyLoansWrittenOff: CreateAmendCommonDividends = CreateAmendCommonDividends(
    customerReference = Some("write off"),
    grossAmount = 13700.55
  )

  val createAmendDividendsRequestBody: CreateAmendDividendsRequestBody = CreateAmendDividendsRequestBody(
    foreignDividend = Some(foreignDividend),
    dividendIncomeReceivedWhilstAbroad = Some(dividendIncomeReceivedWhilstAbroad),
    stockDividend = Some(stockDividend),
    redeemableShares = Some(redeemableShares),
    bonusIssuesOfSecurities = Some(bonusIssuesOfSecurities),
    closeCompanyLoansWrittenOff = Some(closeCompanyLoansWrittenOff)
  )

  val requestData: CreateAmendDividendsRequest = CreateAmendDividendsRequest(
    nino = parsedNino,
    taxYear = TaxYear.fromMtd(taxYear),
    body = createAmendDividendsRequestBody
  )

  "CreateAmendDividendsController" should {
    "return a successful response with status OK" when {
      "happy path" in new Test {
        MockedSharedAppConfig.apiGatewayContext.returns("individuals/dividends-income").anyNumberOfTimes()

        willUseValidator(returningSuccess(requestData))

        MockCreateAmendDividendsService
          .createAmendDividends(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(
          expectedStatus = OK,
          maybeAuditRequestBody = Some(validRequestJson),
          maybeExpectedResponseBody = None,
          maybeAuditResponseBody = None)
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTestWithAudit(NinoFormatError, maybeAuditRequestBody = Some(validRequestJson))
      }

      "service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockCreateAmendDividendsService
          .createAmendDividends(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTestWithAudit(RuleTaxYearNotSupportedError, maybeAuditRequestBody = Some(validRequestJson))
      }
    }
  }

  trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller = new CreateAmendDividendsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockCreateAmendDividendsValidatorFactory,
      service = mockCreateAmendDividendsService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    protected def callController(): Future[Result] = controller.createAmendDividends(validNino, taxYear)(fakeRequest.withBody(validRequestJson))

    def event(auditResponse: AuditResponse, requestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateAmendDividendsIncome",
        transactionName = "create-amend-dividends-income",
        detail = GenericAuditDetail(
          userType = "Individual",
          versionNumber = apiVersion.name,
          agentReferenceNumber = None,
          params = Map("nino" -> validNino, "taxYear" -> taxYear),
          requestBody = requestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

  }

}
