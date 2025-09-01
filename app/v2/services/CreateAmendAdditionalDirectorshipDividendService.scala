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

package v2.services

import cats.implicits.toBifunctorOps
import shared.controllers.RequestContext
import shared.models.errors._
import shared.services.{BaseService, ServiceOutcome}
import v2.connectors.CreateAmendAdditionalDirectorshipDividendConnector
import v2.models.request.createAmendAdditionalDirectorshipDividend.CreateAmendAdditionalDirectorshipDividendRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateAmendAdditionalDirectorshipDividendService @Inject() (connector: CreateAmendAdditionalDirectorshipDividendConnector) extends BaseService {

  def createAmend(
      request: CreateAmendAdditionalDirectorshipDividendRequest)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] =
    connector.createAmend(request).map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))

  private val downstreamErrorMap: Map[String, MtdError] = Map(
    "1215" -> NinoFormatError,
    "1117" -> TaxYearFormatError,
    "1217" -> EmploymentIdFormatError,
    "1000" -> InternalError,
    "1216" -> InternalError,
    "5010" -> NotFoundError,
    "1212" -> InternalError,
    "1213" -> InternalError,
    "1218" -> InternalError
  )

}
