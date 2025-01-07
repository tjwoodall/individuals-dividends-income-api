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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.utils.UnitSpec
import v1.models.request.createAmendUkDividendsIncomeAnnualSummary.{
  CreateAmendUkDividendsIncomeAnnualSummaryBody,
  CreateAmendUkDividendsIncomeAnnualSummaryRequest
}

class CreateAmendUkDividendsIncomeAnnualSummaryValidatorSpec extends UnitSpec {

  private val validNino             = "AA123456A"
  private val validTaxYear          = "2019-20"
  private val validUkDividends      = 55844806400.99
  private val validOtherUkDividends = 60267421355.99

  private val validRequestBodyJson: JsValue = Json.parse(s"""
                                                              |{
                                                              | "ukDividends": $validUkDividends,
                                                              | "otherUkDividends": $validOtherUkDividends
                                                              |}
                                                              |""".stripMargin)

  private val emptyRequestBodyJson: JsValue = Json.parse("""{}""")

  private val nonsenseRequestBodyJson: JsValue = Json.parse("""{"field": "value"}""")

  private val nonValidRequestBodyJson: JsValue = Json.parse(
    """
        |{
        |  "ukDividends": true
        |}
    """.stripMargin
  )

  private val invalidUkDividendsJson: JsValue = Json.parse(s"""
                                                                |{
                                                                |  "ukDividends": -1,
                                                                |  "otherUkDividends": $validOtherUkDividends
                                                                |}
                                                                |""".stripMargin)

  private val invalidOtherUkDividendsJson: JsValue = Json.parse(s"""
                                                                     |{
                                                                     |  "ukDividends": $validUkDividends,
                                                                     |  "otherUkDividends": -1
                                                                     |}
                                                                     |""".stripMargin)

  val validRawRequestBody: AnyContentAsJson                   = AnyContentAsJson(validRequestBodyJson)
  val emptyRawRequestBody: AnyContentAsJson                   = AnyContentAsJson(emptyRequestBodyJson)
  val nonsenseRawRequestBody: AnyContentAsJson                = AnyContentAsJson(nonsenseRequestBodyJson)
  val nonValidRawRequestBody: AnyContentAsJson                = AnyContentAsJson(nonValidRequestBodyJson)
  val invalidUkDividendsRawRequestBody: AnyContentAsJson      = AnyContentAsJson(invalidUkDividendsJson)
  val invalidOtherUkDividendsRawRequestBody: AnyContentAsJson = AnyContentAsJson(invalidOtherUkDividendsJson)

  private implicit val correlationId: String = "1234"

  def validator(nino: String, taxYear: String, body: JsValue) = new CreateAmendUkDividendsIncomeAnnualSummaryValidator(nino, taxYear, body)

  private val parsedNino    = Nino(validNino)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)

  "running validation" should {
    "return no errors" when {
      "passed a valid raw request model" in {
        val result: Either[ErrorWrapper, CreateAmendUkDividendsIncomeAnnualSummaryRequest] =
          validator(validNino, validTaxYear, validRequestBodyJson).validateAndWrapResult()

        val createAmendUkDividendsIncomeAnnualSummaryBody =
          CreateAmendUkDividendsIncomeAnnualSummaryBody(Some(validUkDividends), Some(validOtherUkDividends))

        result shouldBe Right(
          CreateAmendUkDividendsIncomeAnnualSummaryRequest(parsedNino, parsedTaxYear, createAmendUkDividendsIncomeAnnualSummaryBody))

      }
    }

    "return NinoFormatError error" when {
      "passed an invalid nino" in {
        val result: Either[ErrorWrapper, CreateAmendUkDividendsIncomeAnnualSummaryRequest] =
          validator("A12344A", validTaxYear, validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))

      }
    }

    "return TaxYearFormatError error" when {
      "passed an invalid taxYear" in {
        val result: Either[ErrorWrapper, CreateAmendUkDividendsIncomeAnnualSummaryRequest] =
          validator(validNino, "201920", validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))

      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an invalid tax year is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendUkDividendsIncomeAnnualSummaryRequest] =
          validator(validNino, "2016-17", validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))

      }
    }

    "return RuleTaxYearRangeInvalidError error" when {
      "an invalid tax year is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendUkDividendsIncomeAnnualSummaryRequest] =
          validator(validNino, "2019-23", validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))

      }
    }

    "return RuleIncorrectOrEmptyBodyError error" when {
      "an empty JSON body is submitted" in {
        val result: Either[ErrorWrapper, CreateAmendUkDividendsIncomeAnnualSummaryRequest] =
          validator(validNino, validTaxYear, emptyRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      "a non-empty JSON body is submitted without any expected fields" in {
        val result: Either[ErrorWrapper, CreateAmendUkDividendsIncomeAnnualSummaryRequest] =
          validator(validNino, validTaxYear, nonsenseRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))

      }

      "the submitted request body is not in the correct format" in {
        val result: Either[ErrorWrapper, CreateAmendUkDividendsIncomeAnnualSummaryRequest] =
          validator(validNino, validTaxYear, nonValidRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq("/ukDividends")))))

      }
    }

    "return ukDividendsFormatError" when {
      "passed invalid ukDividends" in {
        val result: Either[ErrorWrapper, CreateAmendUkDividendsIncomeAnnualSummaryRequest] =
          validator(validNino, validTaxYear, invalidUkDividendsJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.copy(paths = Some(Seq("/ukDividends")))))

      }

      "return otherUkDividendsFormatError" when {
        "passed invalid otherUkDividends" in {
          val result: Either[ErrorWrapper, CreateAmendUkDividendsIncomeAnnualSummaryRequest] =
            validator(validNino, validTaxYear, invalidOtherUkDividendsJson).validateAndWrapResult()

          result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.copy(paths = Some(Seq("/otherUkDividends")))))
        }
      }

    }
  }

}
