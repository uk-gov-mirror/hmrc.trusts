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

package uk.gov.hmrc.trusts.controllers

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers.status
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.connectors.BaseSpec
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.services.{DesService, ValidationService}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models._
import org.mockito.Mockito._
import scala.concurrent.Future

class RegisterTrustControllerSpec extends BaseSpec with GuiceOneServerPerSuite {

  val mockDesService = mock[DesService]
  lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  lazy val validatationService: ValidationService = new ValidationService()

  ".registration" should {

    "return 200 with TRN" when {
      "the register endpoint is called with a valid json payload " in {
        val SUT = new RegisterTrustController(mockDesService, appConfig,validatationService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.successful(RegistrationTrustResponse("XTRN123456")))
        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe OK
        (contentAsJson(result) \ "trn").as[String] mustBe "XTRN123456"
      }
    }


    "return a Conflict" when {
      "trusts is already registered with provided details." in {
        val SUT = new RegisterTrustController(mockDesService, appConfig,validatationService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(AlreadyRegisteredException))
        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe CONFLICT
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "ALREADY_REGISTERED"
        (output \ "message").as[String] mustBe "The trust is already registered."
      }
    }


    "return a BAD REQUEST" when {
      "input request fails schema validation"  in {
        val SUT = new RegisterTrustController(mockDesService, appConfig,validatationService)
        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(invalidRegistrationRequestJson)))
        status(result) mustBe BAD_REQUEST
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."
      }
    }


    "return an internal server error" when {
      "the register endpoint called and something goes wrong." in {
        val SUT = new RegisterTrustController(mockDesService, appConfig,validatationService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(InternalServerErrorException))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
      }
    }

    "return an internal server error" when {
      "the des returns BAD REQUEST" in {
        val SUT = new RegisterTrustController(mockDesService, appConfig,validatationService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(BadRequestException))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
      }
    }

    "return an internal server error" when {
      "the des returns Service Unavailable as dependent service is down. " in {
        val SUT = new RegisterTrustController(mockDesService, appConfig,validatationService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(ServiceNotAvailableException))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
      }
    }
  }
}
