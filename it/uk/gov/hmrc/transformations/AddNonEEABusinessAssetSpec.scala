/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.transformations

import connector.TrustsConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.get_trust.GetTrustSuccessResponse
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{Assertion, AsyncFreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.itbase.IntegrationTestBase
import utils.{JsonUtils, NonTaxable5MLDFixtures}

import scala.concurrent.Future

class AddNonEEABusinessAssetSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with IntegrationTestBase {

  lazy val getTrustResponseFromDES: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromString(NonTaxable5MLDFixtures.DES.newGet5MLDTrustNonTaxableResponse).as[GetTrustSuccessResponse]

  lazy val expectedInitialGetJson: JsValue =
    NonTaxable5MLDFixtures.Trusts.newGetTransformedNonTaxableTrustResponse


  "an add NonEEABusiness Asset call" - {
    val newNonEEABusinessJson = (
      """
        |{
        |  "address": {
        |      "line1": "Line 1",
        |      "line2": "Line 2",
        |      "postCode": "NE1 1NE",
        |      "country": "GB"
        |  },
        |  "govLawCountry": "GB",
        |  "startDate": "2002-01-01",
        |  "lineNo": "1",
        |  "orgName": "TestOrg"
        |}
        |""".stripMargin
      )

    lazy val expectedGetAfterAddNonEEABusinessJson: JsValue =
      JsonUtils.getJsonValueFromFile("5MLD/NonTaxable/add-nonEEABusiness-asset-after-etmp-call.json")

    val stubbedTrustsConnector = mock[TrustsConnector]
    when(stubbedTrustsConnector.getTrustInfo(any())).thenReturn(Future.successful(getTrustResponseFromDES))

    val application = applicationBuilder
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation)),
        bind[TrustsConnector].toInstance(stubbedTrustsConnector)
      )
      .build()

    "must return add data in a subsequent 'get' call with a UTR" in assertMongoTest(application) { application =>
      runTest("5465416546", application)
      runTest("0123456789ABCDE", application)
    }

    def runTest(identifier: String, application: Application): Assertion = {
      val result = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(result) mustBe OK
      contentAsJson(result) mustBe expectedInitialGetJson

      val addRequest = FakeRequest(POST, s"/trusts/assets/add-non-eea-business/$identifier")
        .withBody(newNonEEABusinessJson)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val addResult = route(application, addRequest).get
      status(addResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, s"/trusts/$identifier/transformed")).get
      status(newResult) mustBe OK
      contentAsJson(newResult) mustBe expectedGetAfterAddNonEEABusinessJson
    }
  }
}
