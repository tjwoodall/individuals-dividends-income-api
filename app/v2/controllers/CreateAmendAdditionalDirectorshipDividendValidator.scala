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

import api.controllers.validators.Validator
import api.controllers.validators.resolvers.{ResolveEmploymentId, ResolveNino, ResolveNonEmptyJsonObject, ResolveTaxYearMinimum}
import api.models.domain.TaxYear
import api.models.errors.MtdError
import cats.data.Validated
import cats.implicits.catsSyntaxTuple4Semigroupal
import play.api.libs.json.JsValue
import v2.models.request.createAmendAdditionalDirectorshipDividend.*

class CreateAmendAdditionalDirectorshipDividendValidator(nino: String, taxYear: String, employmentId: String, body: JsValue)
    extends Validator[CreateAmendAdditionalDirectorshipDividendRequest] {

  private val resolveJson = ResolveNonEmptyJsonObject.resolver[CreateAmendAdditionalDirectorshipDividendRequestBody]

  private val resolveTaxYear = ResolveTaxYearMinimum(TaxYear.fromMtd("2025-26"))

  override def validate: Validated[Seq[MtdError], CreateAmendAdditionalDirectorshipDividendRequest] =
    (
      ResolveNino(nino),
      resolveTaxYear(taxYear),
      ResolveEmploymentId(employmentId),
      resolveJson(body)
    ).mapN(
      CreateAmendAdditionalDirectorshipDividendRequest.apply) andThen CreateAmendAdditionalDirectorshipDividendRulesValidator.validateBusinessRules

}
