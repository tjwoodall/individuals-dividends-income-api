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

package v1.connectors

import config.DividendsIncomeFeatureSwitches
import play.api.libs.json.JsObject
import shared.config.SharedAppConfig
import shared.connectors.DownstreamUri.{DesUri, IfsUri, TaxYearSpecificIfsUri}
import shared.connectors.httpparsers.StandardDownstreamHttpParser._
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import v1.models.request.deleteUkDividendsIncomeAnnualSummary.DeleteUkDividendsIncomeAnnualSummaryRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteUkDividendsIncomeAnnualSummaryConnector @Inject() (val http: HttpClientV2, val appConfig: SharedAppConfig)
    extends BaseDownstreamConnector {

  def delete(request: DeleteUkDividendsIncomeAnnualSummaryRequest)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[Unit]] = {

    import request._

    val intent = if (DividendsIncomeFeatureSwitches().isPassDeleteIntentEnabled) Some("DELETE") else None

    val path = s"income-tax/nino/${nino.nino}/income-source/dividends/annual/${taxYear.asDownstream}"

    if (taxYear.useTaxYearSpecificApi) {
      delete(
        uri = TaxYearSpecificIfsUri[Unit](s"income-tax/${taxYear.asTysDownstream}/${nino.nino}/income-source/dividends/annual")
      )
    } else if (DividendsIncomeFeatureSwitches().isDesIfMigrationEnabled) {
      post(
        body = JsObject.empty,
        uri = IfsUri[Unit](path),
        intent
      )
    } else {
      post(
        body = JsObject.empty,
        uri = DesUri[Unit](path),
        intent
      )
    }
  }

}
