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

package v2.fixtures

import play.api.libs.json.{JsValue, Json}
import v2.models.request.createAmendAdditionalDirectorshipDividend.CreateAmendAdditionalDirectorshipDividendRequestBody

object CreateAmendAdditionalDirectorshipDividendFixtures {

  val fullRequestBodyModel: CreateAmendAdditionalDirectorshipDividendRequestBody =
    CreateAmendAdditionalDirectorshipDividendRequestBody(
      companyDirector = true,
      closeCompany = Some(true),
      directorshipCeasedDate = Some("2025-07-01"),
      companyName = Some("Company One"),
      companyNumber = Some("36488522"),
      shareholding = Some(20.95),
      dividendReceived = Some(1024.99)
    )

  val minimumRequestBodyModel: CreateAmendAdditionalDirectorshipDividendRequestBody =
    CreateAmendAdditionalDirectorshipDividendRequestBody(
      companyDirector = false,
      closeCompany = None,
      directorshipCeasedDate = None,
      companyName = None,
      companyNumber = None,
      shareholding = None,
      dividendReceived = None
    )

  val validFullRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "companyDirector": true,
      |  "closeCompany": true,
      |  "directorshipCeasedDate": "2025-07-01",
      |  "companyName": "Company One",
      |  "companyNumber": "36488522",
      |  "shareholding": 20.95,
      |  "dividendReceived": 1024.99
      |}
    """.stripMargin
  )

  val validMinimumRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "companyDirector": false
      |}
    """.stripMargin
  )

}
