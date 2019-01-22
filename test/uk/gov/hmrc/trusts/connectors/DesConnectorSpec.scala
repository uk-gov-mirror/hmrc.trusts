/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.models.ExistingTrustResponse._
import uk.gov.hmrc.trusts.models.{ErrorRegistrationTrustsResponse, ExistingTrustCheckRequest, ExistingTrustResponse, SuccessRegistrationResponse}
import uk.gov.hmrc.trusts.utils.WireMockHelper

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DesConnectorSpec extends BaseConnectorSpec
  with GuiceOneAppPerSuite with WireMockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Seq("microservice.services.des.port" -> wiremockPort,
        "auditing.enabled" -> false): _*).build()


  lazy val connector: DesConnector = app.injector.instanceOf[DesConnector]

  lazy val request = ExistingTrustCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")


  ".checkExistingTrust" should {

    "return Matched " when {
      "trusts data match with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 200, """{"match": true}""")

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe Matched
      }
    }
    "return NotMatched " when {
      "trusts data does not with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 200, """{"match": false}""")

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe NotMatched
      }
    }

    "return BadRequest " when {
      "payload sent is not valid" in {
        val wrongPayloadRequest = request.copy(utr = "NUMBER1234")
        val requestBody = Json.stringify(Json.toJson(wrongPayloadRequest))

        stubFor("/trusts/match", requestBody, 400, Json.stringify(jsonResponse400))

        val result = Await.result(connector.checkExistingTrust(wrongPayloadRequest), Duration.Inf)
        result mustBe BadRequest
      }
    }

    "return AlreadyRegistered " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 409, Json.stringify(jsonResponseAlreadyRegistered))

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe AlreadyRegistered
      }
    }

    "return ServiceUnavailable " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 503, Json.stringify(jsonResponse503))

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe ServiceUnavailable
      }
    }

    "return ServerError " when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 500, Json.stringify(jsonResponse500))

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe ServerError
      }
    }

    "return ServerError " when {
      "des is returning forbidden response" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 409, "{}")

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe ServerError
      }
    }
  }


    ".registerTrust" should {

      "return TRN  " when {
        "valid request to des register trust." in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))

          stubFor("/trusts/registration", requestBody, 200, """{"trn": "XTRN1234567"}""")

          val result = Await.result(connector.registerTrust(registrationRequest), Duration.Inf)
          result mustBe SuccessRegistrationResponse("XTRN1234567")
        }
      }


      "return ErrorRegistrationTrustsResponse with BAD_REQUEST as code " when {
        "payload sent to des is invalid" in {
          val requestBody = Json.stringify(Json.toJson(invalidRegistrationRequest))
          stubFor("/trusts/registration", requestBody, 400, Json.stringify(jsonResponse400))

          val result = Await.result(connector.registerTrust(invalidRegistrationRequest), Duration.Inf)
          result mustBe ErrorRegistrationTrustsResponse("BAD_REQUEST", "Invalid payload submitted.")
          result.asInstanceOf[ErrorRegistrationTrustsResponse].code mustBe "BAD_REQUEST"

        }
      }

      "return ErrorRegistrationTrustsResponse with ALREADY_REGISTERED code " when {
        "trusts is already registered with provided details." in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))

          stubFor("/trusts/registration", requestBody, 403, Json.stringify(jsonResponseAlreadyRegistered))

          val result = Await.result(connector.registerTrust(registrationRequest), Duration.Inf)
          result mustBe ErrorRegistrationTrustsResponse("ALREADY_REGISTERED", "Trust is already registered.")
          result.asInstanceOf[ErrorRegistrationTrustsResponse].code mustBe "ALREADY_REGISTERED"

        }
      }

      "return ErrorRegistrationTrustsResponse with code SERVIVE_UNAVAILABLE " when {
        "des dependent service is not responding " in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))

          stubFor("/trusts/registration", requestBody, 503, Json.stringify(jsonResponse503))

          val result = Await.result(connector.registerTrust(registrationRequest), Duration.Inf)
          result mustBe ErrorRegistrationTrustsResponse("SERVIVE_UNAVAILABLE", "Depedent system is not responding.")
          result.asInstanceOf[ErrorRegistrationTrustsResponse].code mustBe "SERVIVE_UNAVAILABLE"
        }
      }

      "return ErrorRegistrationTrustsResponse with code INTERNAL_SERVER_ERROR " when {
        "des is experiencing some problem." in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))

          stubFor("/trusts/registration", requestBody, 500, Json.stringify(jsonResponse500))

          val result = Await.result(connector.registerTrust(registrationRequest), Duration.Inf)
          result mustBe ErrorRegistrationTrustsResponse("INTERNAL_SERVER_ERROR", "Internal server error.")
          result.asInstanceOf[ErrorRegistrationTrustsResponse].code mustBe "INTERNAL_SERVER_ERROR"

        }
      }

      "return ErrorRegistrationTrustsResponse with INTERNAL_SERVER_ERROR" when {
        "des is returning 403 without ALREADY REGISTERED code." in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))

          stubFor("/trusts/registration", requestBody, 403, "{}")

          val result = Await.result(connector.registerTrust(registrationRequest), Duration.Inf)
          result mustBe ErrorRegistrationTrustsResponse("INTERNAL_SERVER_ERROR", "Internal server error.")
          result.asInstanceOf[ErrorRegistrationTrustsResponse].code mustBe "INTERNAL_SERVER_ERROR"

        }
      }

      "return ErrorRegistrationTrustsResponse with INTERNAL_SERVER_ERROR" when {
        "des is down" in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))
          server.stop()
          val result = Await.result(connector.registerTrust(registrationRequest), Duration.Inf)
          result mustBe ErrorRegistrationTrustsResponse("INTERNAL_SERVER_ERROR", "Internal server error.")
          result.asInstanceOf[ErrorRegistrationTrustsResponse].code mustBe "INTERNAL_SERVER_ERROR"
          server.start()
        }
      }

      "return ErrorRegistrationTrustsResponse with INTERNAL_SERVER_ERROR" when {
        "there is gateway timeout before receiving response" in {
          val requestBody = Json.stringify(Json.toJson(registrationRequest))
          stubFor("/trusts/registration", requestBody, 200, """{"trn": "XTRN1234567"}""",25000)
          val result = Await.result(connector.registerTrust(registrationRequest), Duration.Inf)
          result mustBe ErrorRegistrationTrustsResponse("INTERNAL_SERVER_ERROR", "Internal server error.")
          result.asInstanceOf[ErrorRegistrationTrustsResponse].code mustBe "INTERNAL_SERVER_ERROR"

        }
      }







    }


  def stubFor(url: String, requestBody: String, returnStatus: Int, responseBody: String, delayResponse :Int = 0) = {
    server.stubFor(post(urlEqualTo(url))
      .withHeader("content-Type", containing("application/json"))
      .withHeader("Environment", containing("dev"))
      .withRequestBody(equalTo(requestBody))
      .willReturn(
        aResponse()
          .withStatus(returnStatus)
          .withBody(responseBody).withFixedDelay(delayResponse)))
  }





}//end