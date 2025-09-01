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

import cats.data.Validated
import shared.controllers.validators.RulesValidator
import shared.controllers.validators.resolvers.ResolveParsedNumber
import shared.models.errors.MtdError
import v1.models.request.createAmendUkDividendsIncomeAnnualSummary.CreateAmendUkDividendsIncomeAnnualSummaryRequest

object CreateAmendUkDividendsIncomeAnnualSummaryRulesValidator extends RulesValidator[CreateAmendUkDividendsIncomeAnnualSummaryRequest] {

  private val resolveParsedNumber = ResolveParsedNumber()

  override def validateBusinessRules(
      parsed: CreateAmendUkDividendsIncomeAnnualSummaryRequest): Validated[Seq[MtdError], CreateAmendUkDividendsIncomeAnnualSummaryRequest] = {
    import parsed.body._

    combine(
      resolveParsedNumber(ukDividends, "/ukDividends"),
      resolveParsedNumber(otherUkDividends, "/otherUkDividends")
    ).onSuccess(parsed)
  }

}
