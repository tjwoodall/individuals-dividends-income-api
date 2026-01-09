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
import cats.data.Validated.Invalid
import cats.implicits.toTraverseOps
import common.errors.CustomerRefFormatError
import shared.controllers.validators.RulesValidator
import shared.controllers.validators.resolvers.{ResolveParsedCountryCode, ResolveParsedNumber}
import shared.models.errors.MtdError
import v1.models.request.createAmendDividends._

object CreateAmendDividendsRulesValidator extends RulesValidator[CreateAmendDividendsRequest] {

  private val resolveParsedNumber: ResolveParsedNumber = ResolveParsedNumber()

  override def validateBusinessRules(parsed: CreateAmendDividendsRequest): Validated[Seq[MtdError], CreateAmendDividendsRequest] = {
    import parsed.body._

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
    import foreignDividend._

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
    import dividendIncomeReceivedWhilstAbroad._

    combine(
      ResolveParsedCountryCode(countryCode, s"/dividendIncomeReceivedWhilstAbroad/$arrayIndex/countryCode"),
      resolveParsedNumber(amountBeforeTax, s"/dividendIncomeReceivedWhilstAbroad/$arrayIndex/amountBeforeTax"),
      resolveParsedNumber(taxTakenOff, s"/dividendIncomeReceivedWhilstAbroad/$arrayIndex/taxTakenOff"),
      resolveParsedNumber(specialWithholdingTax, s"/dividendIncomeReceivedWhilstAbroad/$arrayIndex/specialWithholdingTax"),
      resolveParsedNumber(taxableAmount, s"/dividendIncomeReceivedWhilstAbroad/$arrayIndex/taxableAmount")
    )
  }

  private def validateCommonDividends(commonDividends: CreateAmendCommonDividends, fieldName: String): Validated[Seq[MtdError], Unit] = {
    import commonDividends._

    combine(
      validateCustomerRef(customerReference, s"/$fieldName/customerReference"),
      resolveParsedNumber(grossAmount, s"/$fieldName/grossAmount")
    )
  }

  private def validateCustomerRef(customerRef: Option[String], path: String): Validated[Seq[MtdError], Unit] = {
    val stringRegex = "^[0-9a-zA-Z{À-˿’}\\- _&`():.'^]{1,90}$".r

    customerRef match {
      case Some(customerRef) if stringRegex.matches(customerRef) => valid
      case None                                                  => valid
      case _                                                     => Invalid(List(CustomerRefFormatError.withPath(path)))
    }
  }

}
