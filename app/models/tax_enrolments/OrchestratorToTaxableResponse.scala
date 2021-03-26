/*
 * Copyright 2021 HM Revenue & Customs
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

package models.tax_enrolments

import exceptions._
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

sealed trait OrchestratorToTaxableResponse

case object OrchestratorToTaxableSuccess extends OrchestratorToTaxableResponse
case object OrchestratorToTaxableFailure extends OrchestratorToTaxableResponse

object OrchestratorToTaxableResponse extends Logging {

  implicit lazy val httpReads: HttpReads[OrchestratorToTaxableResponse] = (_: String, _: String, response: HttpResponse) => {
    logger.info(s"Response status received from orchestrator: ${response.status}")
    response.status match {
      case NO_CONTENT | OK =>
        OrchestratorToTaxableSuccess
      case BAD_REQUEST =>
        logger.error("Bad request response received from orchestrator")
        throw BadRequestException
      case status =>
        logger.error(s"Error response from orchestrator: $status")
        throw InternalServerErrorException(s"Error response from orchestrator: $status")
    }
  }
}
