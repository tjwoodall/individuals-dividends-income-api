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

import shared.models.domain._
import shared.models.errors._
import shared.utils.UnitSpec
import v2.models.request.retrieveAdditionalDirectorshipDividend.RetrieveAdditionalDirectorshipDividendRequest

class RetrieveAdditionalDirectorshipDividendValidatorSpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino: String         = "AA123456A"
  private val validTaxYear: String      = "2025-26"
  private val validEmploymentId: String = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"

  private val parsedNino: Nino                 = Nino(validNino)
  private val parsedTaxYear: TaxYear           = TaxYear.fromMtd(validTaxYear)
  private val parsedEmploymentId: EmploymentId = EmploymentId(validEmploymentId)

  def validator(nino: String, taxYear: String, employmentId: String): RetrieveAdditionalDirectorshipDividendValidator =
    new RetrieveAdditionalDirectorshipDividendValidator(nino, taxYear, employmentId)

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        val result: Either[ErrorWrapper, RetrieveAdditionalDirectorshipDividendRequest] = validator(validNino, validTaxYear, validEmploymentId).validateAndWrapResult()

        result shouldBe Right(RetrieveAdditionalDirectorshipDividendRequest(parsedNino, parsedTaxYear, parsedEmploymentId))
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        val result: Either[ErrorWrapper, RetrieveAdditionalDirectorshipDividendRequest] = validator("A12344A", validTaxYear, validEmploymentId).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {
        val result: Either[ErrorWrapper, RetrieveAdditionalDirectorshipDividendRequest] = validator(validNino, "20178", validEmploymentId).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }
    }

    "return RuleTaxYearRangeInvalidError error" when {
      "an invalid tax year range is supplied" in {
        val result: Either[ErrorWrapper, RetrieveAdditionalDirectorshipDividendRequest] = validator(validNino, "2019-21", validEmploymentId).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an unsupported tax year is supplied" in {
        val result: Either[ErrorWrapper, RetrieveAdditionalDirectorshipDividendRequest] = validator(validNino, "2018-19", validEmploymentId).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }
    }

    "return EmploymentIdFormatError error" when {
      "an invalid employment ID is supplied" in {
        val result: Either[ErrorWrapper, RetrieveAdditionalDirectorshipDividendRequest] = validator(validNino, validTaxYear, "incorrect-id").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, EmploymentIdFormatError))
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        val result: Either[ErrorWrapper, RetrieveAdditionalDirectorshipDividendRequest] = validator("A12344A", "20178", "incorrect-id").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(List(EmploymentIdFormatError, NinoFormatError, TaxYearFormatError))))
      }
    }
  }

}
