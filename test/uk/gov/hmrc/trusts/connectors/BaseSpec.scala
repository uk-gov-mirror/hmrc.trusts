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

import org.scalatest.{Matchers, MustMatchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.utils.JsonRequests


class BaseSpec extends WordSpec with MustMatchers with ScalaFutures with MockitoSugar with JsonRequests {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  def postRequestWithPayload(payload: JsValue): FakeRequest[JsValue] =
    FakeRequest("POST", "")
      .withHeaders(CONTENT_TYPE -> "application/json")
      .withBody(payload)





}
