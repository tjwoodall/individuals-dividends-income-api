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

package v2.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json._
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers._
import shared.models.errors._
import shared.models.utils.JsonErrorValidators
import shared.services._
import shared.support.IntegrationBaseSpec
import v2.fixtures.CreateAmendAdditionalDirectorshipDividendFixtures.{validFullRequestBodyJson, validMinimumRequestBodyJson}

class CreateAmendAdditionalDirectorshipDividendControllerISpec extends IntegrationBaseSpec with JsonErrorValidators {

  "Calling the 'Create or Amend Additional Directorship and Dividend Information' endpoint" should {
    "return a 204 status code" when {
      "any valid request is made" in new Test  {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, downstreamQueryParam, NO_CONTENT, JsObject.empty)
        }

        val response: WSResponse = await(request().put(validFullRequestBodyJson))
        response.status shouldBe NO_CONTENT
        response.body shouldBe ""
      }
    }

    "return error according to spec" when {

      val incorrectlyFormattedRequestBodyJson: JsValue = Json.parse(
        s"""
          |{
          |  "companyDirector": true,
          |  "closeCompany": true,
          |  "directorshipCeasedDate": "2024",
          |  "companyName": "${"a" * 161}",
          |  "companyNumber": "12ABC456",
          |  "shareholding": -20.95,
          |  "dividendReceived": -1024.99
          |}
        """.stripMargin
      )

      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestEmploymentId: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: MtdError,
                                errorWrapper: Option[ErrorWrapper]): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {

            override val nino: String         = requestNino
            override val taxYear: String      = requestTaxYear
            override val employmentId: String = requestEmploymentId

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val expectedBodyJson: JsValue = errorWrapper match {
              case Some(wrapper) => Json.toJson(wrapper)
              case None          => Json.toJson(expectedBody)
            }

            val response: WSResponse = await(request().put(requestBody))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBodyJson)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = Seq(
          ("AA1123A", "2025-26", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validFullRequestBodyJson, BAD_REQUEST, NinoFormatError, None),
          ("AA123456A", "2025", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",  validFullRequestBodyJson, BAD_REQUEST, TaxYearFormatError, None),
          ("AA123456A", "2025-26",  "ABCDE12345FG", validFullRequestBodyJson, BAD_REQUEST, EmploymentIdFormatError, None),
          ("AA123456A", "2025-27",  "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validFullRequestBodyJson, BAD_REQUEST, RuleTaxYearRangeInvalidError, None),
          ("AA123456A", "2024-25",  "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validFullRequestBodyJson, BAD_REQUEST, RuleTaxYearNotSupportedError, None),
          ("AA123456A", "2025-26", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", JsObject.empty, BAD_REQUEST, RuleIncorrectOrEmptyBodyError, None),
          (
            "AA123456A",
            "2025-26",
            "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
            validMinimumRequestBodyJson.update("/companyDirector", JsBoolean(true)),
            BAD_REQUEST,
            RuleMissingCloseCompanyError,
            None
          ),
          (
            "AA123456A",
            "2025-26",
            "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
            validMinimumRequestBodyJson
              .update("/companyDirector", JsBoolean(true))
              .update("/closeCompany", JsBoolean(true)),
            BAD_REQUEST,
            RuleMissingCloseCompanyDetailsError.withPaths(
              List("/companyName", "/companyNumber", "/shareholding", "/dividendReceived")
            ),
            None
          ),
          (
            "AA123456A",
            "2025-26",
            "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
            validFullRequestBodyJson.update("/directorshipCeasedDate", JsString("2026-04-06")),
            BAD_REQUEST,
            RuleDirectorshipCeasedDateError,
            None
          ),
          (
            "AA123456A",
            "2025-26",
            "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
            incorrectlyFormattedRequestBodyJson,
            BAD_REQUEST,
            BadRequestError,
            Some(
              ErrorWrapper(
                "123",
                BadRequestError,
                Some(
                  List(
                    CompanyNameFormatError,
                    CompanyNumberFormatError,
                    DirectorshipCeasedDateFormatError,
                    ValueFormatError.withPath("/dividendReceived"),
                    ValueFormatError.forPathAndRange("/shareholding", "0", "100")
                  )
                )
              )
            )
          )
        )

        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns a code $downstreamCode error and status $downstreamStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.PUT, downstreamUri, downstreamQueryParam, downstreamStatus, errorBody(downstreamCode))

            }

            val response: WSResponse = await(request().put(validFullRequestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        def errorBody(code: String): String =
          s"""
            |[
            |  {
            |    "errorCode": "$code",
            |    "errorDescription": "error description"
            |  }
            |]
          """.stripMargin

        val errors = List(
          (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "1217", BAD_REQUEST, EmploymentIdFormatError),
          (BAD_REQUEST, "1000", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "UNMATCHED_STUB_ERROR", BAD_REQUEST, RuleIncorrectGovTestScenarioError),
          (NOT_FOUND, "5010", NOT_FOUND, NotFoundError),
          (UNPROCESSABLE_ENTITY, "1212", INTERNAL_SERVER_ERROR, InternalError),
          (UNPROCESSABLE_ENTITY, "1213", INTERNAL_SERVER_ERROR, InternalError),
          (UNPROCESSABLE_ENTITY, "1218", INTERNAL_SERVER_ERROR, InternalError)
        )

        errors.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }

  private trait Test {

    val nino: String         = "AA123456A"
    val employmentId: String = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"

    def taxYear: String = "2025-26"

    def downstreamQueryParam: Map[String, String] = Map("taxYear" -> "25-26")

    def downstreamUri: String = s"/itsd/income-sources/$nino/directorships/$employmentId"

    private def uri: String = s"/directorship/$nino/$taxYear/$employmentId"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.2.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

}
