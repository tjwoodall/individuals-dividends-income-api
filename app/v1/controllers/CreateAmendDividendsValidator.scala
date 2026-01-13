/*
 * Copyright 2026 HM Revenue & Customs
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

import cats.data.Validated
import cats.implicits.*
import play.api.libs.json.JsValue
import shared.config.SharedAppConfig
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveNino, ResolveNonEmptyJsonObject, ResolveTaxYearMinimum}
import shared.models.domain.TaxYear
import shared.models.errors.MtdError
import v1.models.request.createAmendDividends.*

import javax.inject.Inject

class CreateAmendDividendsValidator @Inject() (nino: String, taxYear: String, body: JsValue)(implicit appConfig: SharedAppConfig)
    extends Validator[CreateAmendDividendsRequest] {

  private val resolveJson = ResolveNonEmptyJsonObject.resolver[CreateAmendDividendsRequestBody]

  private lazy val minimumTaxYear = appConfig.minimumPermittedTaxYear
  private lazy val resolveTaxYear = ResolveTaxYearMinimum(minimumTaxYear)

  override def validate: Validated[Seq[MtdError], CreateAmendDividendsRequest] =
    (
      ResolveNino(nino),
      resolveTaxYear(taxYear),
      resolveJson(body)
    ).mapN(CreateAmendDividendsRequest.apply) andThen CreateAmendDividendsRulesValidator.validateBusinessRules

}
