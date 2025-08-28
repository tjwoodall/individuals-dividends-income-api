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

import common.errors.CustomerRefFormatError
import play.api.libs.json.{JsValue, Json}
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.utils.UnitSpec
import v2.models.request.createAmendDividends._

class CreateAmendDividendsValidatorSpec extends UnitSpec {

  private val validNino    = "AA123456A"
  private val validTaxYear = "2020-21"

  private val validRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignDividend": [
      |      {
      |        "countryCode": "DEU",
      |        "amountBeforeTax": 1232.22,
      |        "taxTakenOff": 22.22,
      |        "specialWithholdingTax": 27.35,
      |        "foreignTaxCreditRelief": true,
      |        "taxableAmount": 2321.22
      |      },
      |      {
      |        "countryCode": "FRA",
      |        "amountBeforeTax": 1350.55,
      |        "taxTakenOff": 25.27,
      |        "specialWithholdingTax": 30.59,
      |        "foreignTaxCreditRelief": false,
      |        "taxableAmount": 2500.99
      |      }
      |   ],
      |   "dividendIncomeReceivedWhilstAbroad": [
      |      {
      |        "countryCode": "DEU",
      |        "amountBeforeTax": 1232.22,
      |        "taxTakenOff": 22.22,
      |        "specialWithholdingTax": 27.35,
      |        "foreignTaxCreditRelief": true,
      |        "taxableAmount": 2321.22
      |      },
      |      {
      |        "countryCode": "FRA",
      |        "amountBeforeTax": 1350.55,
      |        "taxTakenOff": 25.27,
      |        "specialWithholdingTax": 30.59,
      |        "foreignTaxCreditRelief": false,
      |        "taxableAmount": 2500.99
      |       }
      |   ],
      |   "stockDividend": {
      |      "customerReference": "my divs",
      |      "grossAmount": 12321.22
      |   },
      |   "redeemableShares": {
      |      "customerReference": "my shares",
      |      "grossAmount": 12345.75
      |   },
      |   "bonusIssuesOfSecurities": {
      |      "customerReference": "my secs",
      |      "grossAmount": 12500.89
      |   },
      |   "closeCompanyLoansWrittenOff": {
      |      "customerReference": "write off",
      |      "grossAmount": 13700.55
      |   }
      |}
    """.stripMargin
  )

  private val emptyRequestBodyJson: JsValue = Json.parse("""{}""")

  private val nonsenseRequestBodyJson: JsValue = Json.parse("""{"field": "value"}""")

  private val nonValidRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "stockDividend": {
      |      "customerReference": "my divs",
      |      "grossAmount": "no"
      |   }
      |}
    """.stripMargin
  )

  private val missingMandatoryFieldJson: JsValue = Json.parse(
    """
      |{
      |   "foreignDividend": [
      |      {
      |        "amountBeforeTax": 1232.22,
      |        "taxTakenOff": 22.22,
      |        "specialWithholdingTax": 27.35
      |      },
      |      {
      |        "amountBeforeTax": 1350.55,
      |        "taxTakenOff": 25.27,
      |        "specialWithholdingTax": 30.59
      |      }
      |   ],
      |   "dividendIncomeReceivedWhilstAbroad": [
      |      {
      |        "amountBeforeTax": 1232.22,
      |        "taxTakenOff": 22.22,
      |        "specialWithholdingTax": 27.35
      |      },
      |      {
      |        "amountBeforeTax": 1350.55,
      |        "taxTakenOff": 25.27,
      |        "specialWithholdingTax": 30.59
      |       }
      |   ],
      |   "stockDividend": {
      |      "customerReference": "my divs"
      |   },
      |   "redeemableShares": {
      |      "customerReference": "my shares"
      |   },
      |   "bonusIssuesOfSecurities": {
      |      "customerReference": "my secs"
      |   },
      |   "closeCompanyLoansWrittenOff": {
      |      "customerReference": "write off"
      |   }
      |}
    """.stripMargin
  )

  private val invalidCountryCodeRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignDividend": [
      |      {
      |        "countryCode": "GERMANY",
      |        "amountBeforeTax": 1232.22,
      |        "taxTakenOff": 22.22,
      |        "specialWithholdingTax": 27.35,
      |        "foreignTaxCreditRelief": true,
      |        "taxableAmount": 2321.22
      |      }
      |   ]
      |}
    """.stripMargin
  )

  private val invalidCountryCodeRuleRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "dividendIncomeReceivedWhilstAbroad": [
      |      {
      |        "countryCode": "SBT",
      |        "amountBeforeTax": 1232.22,
      |        "taxTakenOff": 22.22,
      |        "specialWithholdingTax": 27.35,
      |        "foreignTaxCreditRelief": true,
      |        "taxableAmount": 2321.22
      |      }
      |   ]
      |}
    """.stripMargin
  )

  private val invalidCustomerRefRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "redeemableShares": {
      |      "customerReference": "This customer ref string is 91 characters long ------------------------------------------91",
      |      "grossAmount": 12345.75
      |   }
      |}
    """.stripMargin
  )

  private val invalidForeignDividendRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignDividend": [
      |      {
      |        "countryCode": "DEU",
      |        "amountBeforeTax": 1232.223,
      |        "taxTakenOff": 22.22,
      |        "specialWithholdingTax": 27.35,
      |        "foreignTaxCreditRelief": true,
      |        "taxableAmount": 2321.22
      |      }
      |   ]
      |}
    """.stripMargin
  )

  private val invalidDividendIncomeReceivedWhilstAbroadRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "dividendIncomeReceivedWhilstAbroad": [
      |      {
      |        "countryCode": "DEU",
      |        "amountBeforeTax": 1232.22,
      |        "taxTakenOff": -22.22,
      |        "specialWithholdingTax": 27.35,
      |        "foreignTaxCreditRelief": true,
      |        "taxableAmount": 2321.22
      |      }
      |   ]
      |}
    """.stripMargin
  )

  private val invalidStockDividendRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "stockDividend": {
      |      "customerReference": "my divs",
      |      "grossAmount": 12321.224
      |   }
      |}
    """.stripMargin
  )

  private val validStockDividendRequestBodyJsonWithoutCustomerRef: JsValue = Json.parse(
    """
      |{
      |   "stockDividend": {
      |      "grossAmount": 12321.25
      |   }
      |}
    """.stripMargin
  )

  private val invalidRedeemableSharesRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "redeemableShares": {
      |      "customerReference": "my shares",
      |      "grossAmount": -12345.75
      |   }
      |}
    """.stripMargin
  )

  private val validRedeemableSharesRequestBodyJsonWithoutCustomerRef: JsValue = Json.parse(
    """
      |{
      |   "redeemableShares": {
      |      "grossAmount": 12321.25
      |   }
      |}
    """.stripMargin
  )

  private val invalidBonusIssuesOfSecuritiesRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "bonusIssuesOfSecurities": {
      |      "customerReference": "my secs",
      |      "grossAmount": 12500.899
      |   }
      |}
    """.stripMargin
  )

  private val validBonusIssuesOfSecuritiesRequestBodyJsonWithoutCustomerRef: JsValue = Json.parse(
    """
      |{
      |   "bonusIssuesOfSecurities": {
      |      "grossAmount": 12321.25
      |   }
      |}
    """.stripMargin
  )

  private val invalidCloseCompanyLoansWrittenOffRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "closeCompanyLoansWrittenOff": {
      |      "customerReference": "write off",
      |      "grossAmount": -13700.55
      |   }
      |}
    """.stripMargin
  )

  private val validCloseCompanyLoansWrittenOffRequestBodyJsonWithoutCustomerRef: JsValue = Json.parse(
    """
      |{
      |   "closeCompanyLoansWrittenOff": {
      |      "grossAmount": 12321.25
      |   }
      |}
    """.stripMargin
  )

  private val allInvalidValueRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignDividend": [
      |      {
      |        "countryCode": "GERMANY",
      |        "amountBeforeTax": -1232.22,
      |        "taxTakenOff": 22.223,
      |        "specialWithholdingTax": 27.354,
      |        "foreignTaxCreditRelief": true,
      |        "taxableAmount": -2321.22
      |      },
      |      {
      |        "countryCode": "PUR",
      |        "amountBeforeTax": 1350.559,
      |        "taxTakenOff": 25.278,
      |        "specialWithholdingTax": -30.59,
      |        "foreignTaxCreditRelief": false,
      |        "taxableAmount": -2500.99
      |      }
      |   ],
      |   "dividendIncomeReceivedWhilstAbroad": [
      |      {
      |        "countryCode": "FRANCE",
      |        "amountBeforeTax": 1232.227,
      |        "taxTakenOff": 22.224,
      |        "specialWithholdingTax": 27.358,
      |        "foreignTaxCreditRelief": true,
      |        "taxableAmount": 2321.229
      |      },
      |      {
      |        "countryCode": "SBT",
      |        "amountBeforeTax": -1350.55,
      |        "taxTakenOff": -25.27,
      |        "specialWithholdingTax": -30.59,
      |        "foreignTaxCreditRelief": false,
      |        "taxableAmount": -2500.99
      |       }
      |   ],
      |   "stockDividend": {
      |      "customerReference": "This customer ref string is 91 characters long ------------------------------------------91",
      |      "grossAmount": -12321.22
      |   },
      |   "redeemableShares": {
      |      "customerReference": "This customer ref string is 91 characters long ------------------------------------------91",
      |      "grossAmount": 12345.758
      |   },
      |   "bonusIssuesOfSecurities": {
      |      "customerReference": "This customer ref string is 91 characters long ------------------------------------------91",
      |      "grossAmount": -12500.89
      |   },
      |   "closeCompanyLoansWrittenOff": {
      |      "customerReference": "This customer ref string is 91 characters long ------------------------------------------91",
      |      "grossAmount": 13700.557
      |   }
      |}
    """.stripMargin
  )

  private implicit val correlationId: String = "1234"

  def validator(nino: String, taxYear: String, body: JsValue): CreateAmendDividendsValidator =
    new CreateAmendDividendsValidator(nino, taxYear, body)

  private val parsedNino    = Nino(validNino)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)

  "running a validation" should {
    "return no errors" when {
      "a valid request with all fields is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, validRequestBodyJson).validateAndWrapResult()

        val createAmendDividendsRequestBody = CreateAmendDividendsRequestBody(
          Some(
            Seq(
              CreateAmendForeignDividendItem(
                countryCode = "DEU",
                amountBeforeTax = Some(1232.22),
                taxTakenOff = Some(22.22),
                specialWithholdingTax = Some(27.35),
                foreignTaxCreditRelief = Some(true),
                taxableAmount = 2321.22
              ),
              CreateAmendForeignDividendItem(
                countryCode = "FRA",
                amountBeforeTax = Some(1350.55),
                taxTakenOff = Some(25.27),
                specialWithholdingTax = Some(30.59),
                foreignTaxCreditRelief = Some(false),
                taxableAmount = 2500.99
              )
            )),
          Some(
            Seq(
              CreateAmendDividendIncomeReceivedWhilstAbroadItem(
                countryCode = "DEU",
                amountBeforeTax = Some(1232.22),
                taxTakenOff = Some(22.22),
                specialWithholdingTax = Some(27.35),
                foreignTaxCreditRelief = Some(true),
                taxableAmount = 2321.22
              ),
              CreateAmendDividendIncomeReceivedWhilstAbroadItem(
                countryCode = "FRA",
                amountBeforeTax = Some(1350.55),
                taxTakenOff = Some(25.27),
                specialWithholdingTax = Some(30.59),
                foreignTaxCreditRelief = Some(false),
                taxableAmount = 2500.99
              )
            )),
          Some(
            CreateAmendCommonDividends(
              customerReference = Some("my divs"),
              grossAmount = 12321.22
            )),
          Some(
            CreateAmendCommonDividends(
              customerReference = Some("my shares"),
              grossAmount = 12345.75
            )),
          Some(
            CreateAmendCommonDividends(
              customerReference = Some("my secs"),
              grossAmount = 12500.89
            )),
          Some(
            CreateAmendCommonDividends(
              customerReference = Some("write off"),
              grossAmount = 13700.55
            ))
        )

        result shouldBe Right(CreateAmendDividendsRequest(parsedNino, parsedTaxYear, createAmendDividendsRequestBody))
      }

      "a valid request with a stock dividend without customer ref is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, validStockDividendRequestBodyJsonWithoutCustomerRef).validateAndWrapResult()

        val createAmendDividendsRequestBody = CreateAmendDividendsRequestBody(
          None,
          None,
          Some(
            CreateAmendCommonDividends(
              None,
              grossAmount = 12321.25
            )),
          None,
          None,
          None
        )

        result shouldBe Right(CreateAmendDividendsRequest(parsedNino, parsedTaxYear, createAmendDividendsRequestBody))
      }

      "a valid request with redeemable shares without customer ref is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, validRedeemableSharesRequestBodyJsonWithoutCustomerRef).validateAndWrapResult()

        val createAmendDividendsRequestBody = CreateAmendDividendsRequestBody(
          None,
          None,
          None,
          Some(
            CreateAmendCommonDividends(
              None,
              grossAmount = 12321.25
            )),
          None,
          None
        )

        result shouldBe Right(CreateAmendDividendsRequest(parsedNino, parsedTaxYear, createAmendDividendsRequestBody))
      }

      "a valid request with bonus issues of security without customer ref is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, validBonusIssuesOfSecuritiesRequestBodyJsonWithoutCustomerRef).validateAndWrapResult()

        val createAmendDividendsRequestBody = CreateAmendDividendsRequestBody(
          None,
          None,
          None,
          None,
          Some(
            CreateAmendCommonDividends(
              None,
              grossAmount = 12321.25
            )),
          None
        )

        result shouldBe Right(CreateAmendDividendsRequest(parsedNino, parsedTaxYear, createAmendDividendsRequestBody))
      }

      "a valid request with close company loans written off without customer ref is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, validCloseCompanyLoansWrittenOffRequestBodyJsonWithoutCustomerRef).validateAndWrapResult()

        val createAmendDividendsRequestBody = CreateAmendDividendsRequestBody(
          None,
          None,
          None,
          None,
          None,
          Some(
            CreateAmendCommonDividends(
              None,
              grossAmount = 12321.25
            ))
        )

        result shouldBe Right(CreateAmendDividendsRequest(parsedNino, parsedTaxYear, createAmendDividendsRequestBody))
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator("A12344A", validTaxYear, validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, "20178", validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }
    }

    "return RuleIncorrectOrEmptyBodyError error" when {
      "an empty JSON body is submitted" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, emptyRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))

      }

      "a non-empty JSON body is submitted without any expected fields" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, nonsenseRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))

      }

      "the submitted request body is not in the correct format" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, nonValidRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq("/stockDividend/grossAmount")))))
      }

      "return RuleTaxYearRangeInvalidError error" when {
        "an invalid tax year range is supplied" in {
          val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
            validator(validNino, "2019-23", validRequestBodyJson).validateAndWrapResult()

          result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
        }
      }

      "return RuleTaxYearNotSupportedError error" when {
        "an invalid tax year is supplied" in {
          val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
            validator(validNino, "2018-19", validRequestBodyJson).validateAndWrapResult()

          result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
        }
      }

      "mandatory fields are not provided" in {
        val paths: Seq[String] = Seq(
          "/bonusIssuesOfSecurities/grossAmount",
          "/closeCompanyLoansWrittenOff/grossAmount",
          "/dividendIncomeReceivedWhilstAbroad/0/countryCode",
          "/dividendIncomeReceivedWhilstAbroad/0/taxableAmount",
          "/dividendIncomeReceivedWhilstAbroad/1/countryCode",
          "/dividendIncomeReceivedWhilstAbroad/1/taxableAmount",
          "/foreignDividend/0/countryCode",
          "/foreignDividend/0/taxableAmount",
          "/foreignDividend/1/countryCode",
          "/foreignDividend/1/taxableAmount",
          "/redeemableShares/grossAmount",
          "/stockDividend/grossAmount"
        )

        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, missingMandatoryFieldJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.copy(paths = Some(paths))))

      }
    }

    "return CountryCodeFormatError error" when {
      "an incorrectly formatted country code is submitted" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, invalidCountryCodeRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, CountryCodeFormatError.copy(paths = Some(List("/foreignDividend/0/countryCode")))))

      }
    }

    "return RuleCountryCodeError error" when {
      "an invalid country code is submitted" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, invalidCountryCodeRuleRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, RuleCountryCodeError.copy(paths = Some(List("/dividendIncomeReceivedWhilstAbroad/0/countryCode")))))

      }
    }

    "return CustomerRefFormatError error" when {
      "an incorrectly formatted customer reference is submitted" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, invalidCustomerRefRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, CustomerRefFormatError.copy(paths = Some(List("/redeemableShares/customerReference")))))

      }
    }

    "return ValueFormatError error (single failure)" when {
      "one field fails value validation (foreign dividend)" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, invalidForeignDividendRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.copy(paths = Some(List("/foreignDividend/0/amountBeforeTax")))))

      }

      "one field fails value validation (dividend income received whilst abroad)" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, invalidDividendIncomeReceivedWhilstAbroadRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, ValueFormatError.copy(paths = Some(List("/dividendIncomeReceivedWhilstAbroad/0/taxTakenOff")))))

      }

      "one field fails value validation (stock dividend)" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, invalidStockDividendRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.copy(paths = Some(List("/stockDividend/grossAmount")))))

      }

      "one field fails value validation (redeemable shares)" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, invalidRedeemableSharesRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.copy(paths = Some(List("/redeemableShares/grossAmount")))))

      }

      "one field fails value validation (bonus issues of securities)" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, invalidBonusIssuesOfSecuritiesRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.copy(paths = Some(List("/bonusIssuesOfSecurities/grossAmount")))))

      }

      "one field fails value validation (close company loans written off)" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, invalidCloseCompanyLoansWrittenOffRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.copy(paths = Some(List("/closeCompanyLoansWrittenOff/grossAmount")))))

      }
    }

    "return ValueFormatError error (multiple failures)" when {
      "multiple fields fail value validation" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator(validNino, validTaxYear, allInvalidValueRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(
              CountryCodeFormatError.copy(paths = Some(List("/foreignDividend/0/countryCode", "/dividendIncomeReceivedWhilstAbroad/0/countryCode"))),
              CustomerRefFormatError.copy(paths = Some(List(
                "/stockDividend/customerReference",
                "/redeemableShares/customerReference",
                "/bonusIssuesOfSecurities/customerReference",
                "/closeCompanyLoansWrittenOff/customerReference"
              ))),
              ValueFormatError.copy(paths = Some(List(
                "/foreignDividend/0/amountBeforeTax",
                "/foreignDividend/0/taxTakenOff",
                "/foreignDividend/0/specialWithholdingTax",
                "/foreignDividend/0/taxableAmount",
                "/foreignDividend/1/amountBeforeTax",
                "/foreignDividend/1/taxTakenOff",
                "/foreignDividend/1/specialWithholdingTax",
                "/foreignDividend/1/taxableAmount",
                "/dividendIncomeReceivedWhilstAbroad/0/amountBeforeTax",
                "/dividendIncomeReceivedWhilstAbroad/0/taxTakenOff",
                "/dividendIncomeReceivedWhilstAbroad/0/specialWithholdingTax",
                "/dividendIncomeReceivedWhilstAbroad/0/taxableAmount",
                "/dividendIncomeReceivedWhilstAbroad/1/amountBeforeTax",
                "/dividendIncomeReceivedWhilstAbroad/1/taxTakenOff",
                "/dividendIncomeReceivedWhilstAbroad/1/specialWithholdingTax",
                "/dividendIncomeReceivedWhilstAbroad/1/taxableAmount",
                "/stockDividend/grossAmount",
                "/redeemableShares/grossAmount",
                "/bonusIssuesOfSecurities/grossAmount",
                "/closeCompanyLoansWrittenOff/grossAmount"
              ))),
              RuleCountryCodeError.copy(paths = Some(List("/foreignDividend/1/countryCode", "/dividendIncomeReceivedWhilstAbroad/1/countryCode")))
            ))
          ))
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors (path parameters)" in {
        val result: Either[ErrorWrapper, CreateAmendDividendsRequest] =
          validator("A12344A", "20178", validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(List(NinoFormatError, TaxYearFormatError))))
      }
    }
  }

}
