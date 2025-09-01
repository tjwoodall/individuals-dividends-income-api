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

import cats.data.Validated
import cats.data.Validated.Invalid
import shared.controllers.validators.RulesValidator
import shared.controllers.validators.resolvers.{ResolveIsoDate, ResolveParsedNumber, ResolveStringPattern}
import shared.models.domain.TaxYear
import shared.models.errors._
import v2.models.request.createAmendAdditionalDirectorshipDividend.CreateAmendAdditionalDirectorshipDividendRequest

import scala.util.matching.Regex

object CreateAmendAdditionalDirectorshipDividendRulesValidator extends RulesValidator[CreateAmendAdditionalDirectorshipDividendRequest] {

  private def resolveParsedNumber(max: BigDecimal): ResolveParsedNumber = ResolveParsedNumber(0, max)

  private val companyNameRegex: Regex   = "^.{0,160}$".r
  private val companyNumberRegex: Regex = "^(?:\\d{8}|[A-Za-z]{2}\\d{6})$".r

  override def validateBusinessRules(
      parsed: CreateAmendAdditionalDirectorshipDividendRequest): Validated[Seq[MtdError], CreateAmendAdditionalDirectorshipDividendRequest] = {
    import parsed.body._

    combine(
      validateCloseCompany(companyDirector, closeCompany),
      validateCloseCompanyDetails(closeCompany, companyName, companyNumber, shareholding, dividendReceived),
      validateNumericFields(shareholding, dividendReceived),
      validateCompanyStringFields(companyName, companyNumber),
      validateDirectorshipCeasedDate(parsed.taxYear, directorshipCeasedDate)
    ).onSuccess(parsed)
  }

  private def validateCloseCompany(companyDirector: Boolean, closeCompany: Option[Boolean]): Validated[Seq[MtdError], Unit] =
    if (companyDirector && closeCompany.isEmpty) Invalid(List(RuleMissingCloseCompanyError)) else valid

  private def validateFieldPresence[A](field: Option[A], path: String): Validated[Seq[MtdError], Unit] =
    field.fold[Validated[Seq[MtdError], Unit]](Invalid(List(RuleMissingCloseCompanyDetailsError.withPath(path))))(_ => valid)

  private def validateCloseCompanyDetails(closeCompany: Option[Boolean],
                                          companyName: Option[String],
                                          companyNumber: Option[String],
                                          shareholding: Option[BigDecimal],
                                          dividendReceived: Option[BigDecimal]): Validated[Seq[MtdError], Unit] =
    closeCompany match {
      case Some(true) =>
        combine(
          validateFieldPresence(companyName, "/companyName"),
          validateFieldPresence(companyNumber, "/companyNumber"),
          validateFieldPresence(shareholding, "/shareholding"),
          validateFieldPresence(dividendReceived, "/dividendReceived")
        )
      case _ => valid
    }

  private def validateNumericFields(shareholding: Option[BigDecimal], dividendReceived: Option[BigDecimal]): Validated[Seq[MtdError], Unit] =
    combine(
      resolveParsedNumber(max = 100)(shareholding, "/shareholding"),
      resolveParsedNumber(max = 99999999999.99)(dividendReceived, "/dividendReceived")
    )

  private def validateCompanyStringFields(companyName: Option[String], companyNumber: Option[String]): Validated[Seq[MtdError], Unit] =
    combine(
      ResolveStringPattern(companyNameRegex, CompanyNameFormatError)(companyName),
      ResolveStringPattern(companyNumberRegex, CompanyNumberFormatError)(companyNumber)
    )

  private def validateDirectorshipCeasedDate(taxYear: TaxYear, directorshipCeasedDate: Option[String]): Validated[Seq[MtdError], Unit] =
    directorshipCeasedDate.fold(valid) { date =>
      ResolveIsoDate(date, DirectorshipCeasedDateFormatError).andThen { parsedDate =>
        if (parsedDate.isBefore(taxYear.startDate) || parsedDate.isAfter(taxYear.endDate)) {
          Invalid(List(RuleDirectorshipCeasedDateError))
        } else {
          valid
        }
      }
    }

}
