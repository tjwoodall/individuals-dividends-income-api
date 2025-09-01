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

import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsString, JsValue}
import shared.models.domain.{EmploymentId, Nino, TaxYear}
import shared.models.errors._
import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec
import v2.fixtures.CreateAmendAdditionalDirectorshipDividendFixtures._
import v2.models.request.createAmendAdditionalDirectorshipDividend.CreateAmendAdditionalDirectorshipDividendRequest

class CreateAmendAdditionalDirectorshipDividendValidatorSpec extends UnitSpec with JsonErrorValidators {

  private implicit val correlationId: String = "1234"

  private val validNino: String         = "AA123456A"
  private val validTaxYear: String      = "2025-26"
  private val validEmploymentId: String = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"

  private val parsedNino: Nino                 = Nino(validNino)
  private val parsedTaxYear: TaxYear           = TaxYear.fromMtd(validTaxYear)
  private val parsedEmploymentId: EmploymentId = EmploymentId(validEmploymentId)

  private def validator(nino: String, taxYear: String, employmentId: String, body: JsValue): CreateAmendAdditionalDirectorshipDividendValidator =
    new CreateAmendAdditionalDirectorshipDividendValidator(nino, taxYear, employmentId, body)

  "running a validation" should {
    "return no errors" when {
      "a full valid request is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, validTaxYear, validEmploymentId, validFullRequestBodyJson).validateAndWrapResult()

        result shouldBe Right(CreateAmendAdditionalDirectorshipDividendRequest(parsedNino, parsedTaxYear, parsedEmploymentId, fullRequestBodyModel))
      }

      "a minimum valid request is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, validTaxYear, validEmploymentId, validMinimumRequestBodyJson).validateAndWrapResult()

        result shouldBe Right(
          CreateAmendAdditionalDirectorshipDividendRequest(parsedNino, parsedTaxYear, parsedEmploymentId, minimumRequestBodyModel))
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator("A12344A", validTaxYear, validEmploymentId, validFullRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, "20256", validEmploymentId, validFullRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }
    }

    "return RuleTaxYearRangeInvalidError error" when {
      "an invalid tax year range is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, "2025-27", validEmploymentId, validFullRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an unsupported tax year is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, "2024-25", validEmploymentId, validFullRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }
    }

    "return EmploymentIdFormatError error" when {
      "an invalid employment ID is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, validTaxYear, "4557ecb5-fd32-48cc-81f5", validFullRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, EmploymentIdFormatError))
      }
    }

    "return RuleIncorrectOrEmptyBodyError error" when {
      "passed an empty body" in {
        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, validTaxYear, validEmploymentId, JsObject.empty).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      "passed a body with a missing mandatory field" in {
        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, validTaxYear, validEmploymentId, validFullRequestBodyJson.removeProperty("/companyDirector")).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath("/companyDirector")))
      }

      validFullRequestBodyJson.as[JsObject].fields.foreach { case (field, _) =>
        s"passed a body with an incorrect type for field $field" in {
          val invalidJson: JsValue = validFullRequestBodyJson.update(s"/$field", JsObject.empty)

          val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
            validator(validNino, validTaxYear, validEmploymentId, invalidJson).validateAndWrapResult()

          result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath(s"/$field")))
        }
      }
    }

    "return ValueFormatError error" when {
      List(("shareholding", "100"), ("dividendReceived", "99999999999.99")).foreach { case (field, maxRange) =>
        s"passed a body with an incorrectly formatted field $field" in {
          val invalidJson: JsValue = validFullRequestBodyJson.update(s"/$field", JsNumber(18.999))

          val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
            validator(validNino, validTaxYear, validEmploymentId, invalidJson).validateAndWrapResult()

          result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.forPathAndRange(s"/$field", "0", maxRange)))
        }
      }
    }

    "return CompanyNameFormatError error" when {
      "passed a body with an incorrectly formatted company name" in {
        val invalidJson: JsValue = validFullRequestBodyJson.update("/companyName", JsString("a" * 161))

        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, validTaxYear, validEmploymentId, invalidJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, CompanyNameFormatError))
      }
    }

    "return CompanyNumberFormatError error" when {
      "passed a body with an incorrectly formatted company number" in {
        val invalidJson: JsValue = validFullRequestBodyJson.update("/companyNumber", JsString("12ABC456"))

        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, validTaxYear, validEmploymentId, invalidJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, CompanyNumberFormatError))
      }
    }

    "return DirectorshipCeasedDateFormatError error" when {
      "passed a body with an incorrectly formatted directorship ceased date" in {
        val invalidJson: JsValue = validFullRequestBodyJson.update("/directorshipCeasedDate", JsString("2025"))

        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, validTaxYear, validEmploymentId, invalidJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, DirectorshipCeasedDateFormatError))
      }
    }

    "return RuleMissingCloseCompanyError error" when {
      "passed a body with only company director set to true" in {
        val invalidJson: JsValue = validMinimumRequestBodyJson.update("/companyDirector", JsBoolean(true))

        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, validTaxYear, validEmploymentId, invalidJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleMissingCloseCompanyError))
      }
    }

    "return RuleMissingCloseCompanyDetailsError error" when {
      List("companyName", "companyNumber", "shareholding", "dividendReceived").foreach { field =>
        s"passed a body with close company set to true and missing the field $field" in {
          val invalidJson: JsValue = validFullRequestBodyJson.removeProperty(s"/$field")

          val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
            validator(validNino, validTaxYear, validEmploymentId, invalidJson).validateAndWrapResult()

          result shouldBe Left(ErrorWrapper(correlationId, RuleMissingCloseCompanyDetailsError.withPath(s"/$field")))
        }
      }
    }

    "return RuleDirectorshipCeasedDateError error" when {
      "passed a body with a directorship ceased date outside the supplied tax year" in {
        val invalidJson: JsValue = validFullRequestBodyJson.update("/directorshipCeasedDate", JsString("2025-04-05"))

        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator(validNino, validTaxYear, validEmploymentId, invalidJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleDirectorshipCeasedDateError))
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        val result: Either[ErrorWrapper, CreateAmendAdditionalDirectorshipDividendRequest] =
          validator("A12344A", "20256", "4557ecb5-fd32-48cc-81f5", validFullRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(List(EmploymentIdFormatError, NinoFormatError, TaxYearFormatError))))
      }
    }
  }

}
