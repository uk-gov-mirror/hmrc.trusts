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

package connectors

import connector.OrchestratorConnector
import exceptions.{BadRequestException, InternalServerErrorException}
import models.orchestrator.{OrchestratorMigrationRequest, OrchestratorMigrationResponse}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.UpstreamErrorResponse

class OrchestratorConnectorSpec extends ConnectorSpecHelper {

  private lazy val connector: OrchestratorConnector = injector.instanceOf[OrchestratorConnector]

  ".migrateToTaxable" should {

    val urn = "NTTRUST00000001"
    val utr = "123456789"

    val request: OrchestratorMigrationRequest = OrchestratorMigrationRequest(urn, utr)
    val requestBody = Json.stringify(Json.toJson(request))

    "return Success" when {
      "tax enrolments successfully subscribed to provided subscription id" in {
        val responseBody = """{"success": "true"}"""
        stubForHeaderlessPost(server, "/trusts-enrolment-orchestrator/orchestration-process", requestBody, OK, responseBody)

        val futureResult = connector.migrateToTaxable(urn, utr)

        whenReady(futureResult) {
          result => result mustBe OrchestratorMigrationResponse("true", None)
        }
      }
    }

    "return UpstreamErrorResponse " when {
      "tax enrolments returns internal server error " in {
        val responseBody = Json.stringify(Json.parse(
          s"""
             |{
             | "code": "SERVICE_UNAVAILABLE",
             | "reason": "Dependent systems are currently not responding"
             |}
             |""".stripMargin))

        stubForHeaderlessPost(server, "/trusts-enrolment-orchestrator/orchestration-process", requestBody, SERVICE_UNAVAILABLE, responseBody)

        val futureResult = connector.migrateToTaxable(urn, utr)

        whenReady(futureResult.failed) {
          result => result mustBe a[UpstreamErrorResponse]
        }
      }
    }
  }

}
