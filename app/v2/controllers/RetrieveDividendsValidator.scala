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

package v2.controllers

import cats.data.Validated
import cats.implicits.*
import shared.config.SharedAppConfig
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveNino, ResolveTaxYearMinimum}
import shared.models.domain.TaxYear
import shared.models.errors.MtdError
import v2.models.request.retrieveDividends.RetrieveDividendsRequest

import javax.inject.Inject

class RetrieveDividendsValidator @Inject(nino: String, taxYear: String) (implicit appConfig: SharedAppConfig)
    extends Validator[RetrieveDividendsRequest] {

  private lazy val minimumTaxYear = appConfig.minimumPermittedTaxYear
  private lazy val resolveTaxYear = ResolveTaxYearMinimum(minimumTaxYear)

  override def validate: Validated[Seq[MtdError], RetrieveDividendsRequest] =
    (
      ResolveNino(nino),
      resolveTaxYear(taxYear)
    ).mapN(RetrieveDividendsRequest.apply)

}
