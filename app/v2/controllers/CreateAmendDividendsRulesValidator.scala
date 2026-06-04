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

import api.controllers.validators.RulesValidator
import api.controllers.validators.resolvers.{ResolveParsedCountryCode, ResolveParsedNumber, ResolveStringPattern}
import api.models.errors.MtdError
import cats.data.Validated
import cats.implicits.toTraverseOps
import common.errors.CustomerRefFormatError
import v2.models.request.createAmendDividends.*

object CreateAmendDividendsRulesValidator extends RulesValidator[CreateAmendDividendsRequest] {

  private val resolveParsedNumber: ResolveParsedNumber = ResolveParsedNumber()

  private val customerReferenceRegex = "^[0-9a-zA-Z{À-˿’}\\- _&`():.'^]{1,90}$".r

  override def validateBusinessRules(parsed: CreateAmendDividendsRequest): Validated[Seq[MtdError], CreateAmendDividendsRequest] = {
    import parsed.body.*

    combine(
      foreignDividend.getOrElse(Seq.empty).zipWithIndex.traverse { case (dividend, arrayIndex) => validateForeignDividend(dividend, arrayIndex) },
      dividendIncomeReceivedWhilstAbroad.getOrElse(Seq.empty).zipWithIndex.traverse { case (dividend, arrayIndex) =>
        validateDividendIncomeReceivedWhilstAbroad(dividend, arrayIndex)
      },
      stockDividend.traverse(validateCommonDividends(_, "stockDividend")),
      redeemableShares.traverse(validateCommonDividends(_, "redeemableShares")),
      bonusIssuesOfSecurities.traverse(validateCommonDividends(_, "bonusIssuesOfSecurities")),
      closeCompanyLoansWrittenOff.traverse(validateCommonDividends(_, "closeCompanyLoansWrittenOff"))
    ).onSuccess(parsed)
  }

  private def validateForeignDividend(foreignDividend: CreateAmendForeignDividendItem, arrayIndex: Int): Validated[Seq[MtdError], Unit] = {
    import foreignDividend.*

    combine(
      ResolveParsedCountryCode(countryCode, s"/foreignDividend/$arrayIndex/countryCode"),
      resolveParsedNumber(amountBeforeTax, s"/foreignDividend/$arrayIndex/amountBeforeTax"),
      resolveParsedNumber(taxTakenOff, s"/foreignDividend/$arrayIndex/taxTakenOff"),
      resolveParsedNumber(specialWithholdingTax, s"/foreignDividend/$arrayIndex/specialWithholdingTax"),
      resolveParsedNumber(taxableAmount, s"/foreignDividend/$arrayIndex/taxableAmount")
    )
  }

  private def validateDividendIncomeReceivedWhilstAbroad(dividendIncomeReceivedWhilstAbroad: CreateAmendDividendIncomeReceivedWhilstAbroadItem,
                                                         arrayIndex: Int): Validated[Seq[MtdError], Unit] = {
    import dividendIncomeReceivedWhilstAbroad.*

    combine(
      ResolveParsedCountryCode(countryCode, s"/dividendIncomeReceivedWhilstAbroad/$arrayIndex/countryCode"),
      resolveParsedNumber(amountBeforeTax, s"/dividendIncomeReceivedWhilstAbroad/$arrayIndex/amountBeforeTax"),
      resolveParsedNumber(taxTakenOff, s"/dividendIncomeReceivedWhilstAbroad/$arrayIndex/taxTakenOff"),
      resolveParsedNumber(specialWithholdingTax, s"/dividendIncomeReceivedWhilstAbroad/$arrayIndex/specialWithholdingTax"),
      resolveParsedNumber(taxableAmount, s"/dividendIncomeReceivedWhilstAbroad/$arrayIndex/taxableAmount")
    )
  }

  private def validateCommonDividends(commonDividends: CreateAmendCommonDividends, fieldName: String): Validated[Seq[MtdError], Unit] = {
    import commonDividends.*

    combine(
      ResolveStringPattern(customerReference, customerReferenceRegex, CustomerRefFormatError.withPath(s"/$fieldName/customerReference")),
      resolveParsedNumber(grossAmount, s"/$fieldName/grossAmount")
    )
  }

}
