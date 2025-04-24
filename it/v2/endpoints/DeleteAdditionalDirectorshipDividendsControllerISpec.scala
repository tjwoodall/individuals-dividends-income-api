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
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors._
import shared.services._
import shared.support.IntegrationBaseSpec

class DeleteAdditionalDirectorshipDividendsControllerISpec extends IntegrationBaseSpec {

  "Calling the 'delete additional directorship dividends' endpoint" should {
    "return a 204 status code" when {
      "any valid request is made" in new Test  {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.DELETE, downstreamUri, Map("taxYear" -> downstreamTaxYear), NO_CONTENT, JsObject.empty)
        }

        val response: WSResponse = await(request().delete())
        response.status shouldBe NO_CONTENT
        response.body shouldBe ""
      }
    }

    "return error according to spec" when {

      "validation error" when {
        def validationErrorTest(requestNino: String, requestTaxYear: String, requestEmploymentId: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {

            override val nino: String    = requestNino
            override val taxYear: String = requestTaxYear
            override val employmentId: String = requestEmploymentId

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().delete())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = Seq(
          ("AA1123A", "2025-26", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", BAD_REQUEST, NinoFormatError),
          ("AA123456A", "20477", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",  BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2025-26",  "ABCDE12345FG", BAD_REQUEST, EmploymentIdFormatError),
          ("AA123456A", "2025-27",  "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", BAD_REQUEST, RuleTaxYearRangeInvalidError),
          ("AA123456A", "2024-25",  "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", BAD_REQUEST, RuleTaxYearNotSupportedError)
        )
        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $downstreamStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.DELETE, downstreamUri, Map("taxYear" -> downstreamTaxYear), downstreamStatus, errorBody(downstreamCode))

            }

            val response: WSResponse = await(request().delete())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        def errorBody(code: String): String =
          s"""
             |[
             |    {
             |        "errorCode": "$code",
             |        "errorDescription": "error description"
             |    }
             |]
          """.stripMargin

        val errors = List(
          (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "1217", BAD_REQUEST, EmploymentIdFormatError),
          (BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, InternalError),
          (NOT_FOUND,   "5010", NOT_FOUND, NotFoundError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError),
          (NOT_FOUND, "UNMATCHED_STUB_ERROR", BAD_REQUEST, RuleIncorrectGovTestScenarioError)
        )

        errors.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }

  private trait Test {

    val nino: String = "AA123456A"
    val employmentId: String = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"
    def taxYear: String       = "2025-26"
    def downstreamTaxYear: String = "25-26"

    def downstreamUri: String = s"/itsd/income-sources/$nino/directorships/$employmentId"

    def uri: String = s"/directorship/$nino/$taxYear/$employmentId"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.2.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

  }

}
