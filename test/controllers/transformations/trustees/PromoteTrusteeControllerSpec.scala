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

package controllers.transformations.trustees

import controllers.actions.FakeIdentifierAction
import models.NameType
import models.variation._
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import services.{LocalDateService, TransformationService}
import transformers.trustees.PromoteTrusteeTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class PromoteTrusteeControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers
 with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)
  
  private val utr: String = "utr"
  private val index: Int = 0
  private val endDate: LocalDate = LocalDate.parse("2021-01-01")

  private val invalidBody: JsValue = Json.parse("{}")

  private def buildInputJson(trusteeData: Seq[JsValue], isTaxable: Option[Boolean]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val addTrustees = (__ \ "details" \ "trust" \ "entities" \ "trustees").json.put(JsArray(trusteeData))

    val addTrustTaxable = isTaxable match {
      case Some(value) => (__ \ "details" \ "trust" \ "details" \ "trustTaxable").json.put(JsBoolean(value))
      case None => __.json.pick[JsObject]
    }

    baseJson.as[JsObject](__.json.update(addTrustees) andThen __.json.update(addTrustTaxable))
  }
  
  "Promote trustee controller" - {

    "individual trustee" - {

      val originalTrustee = TrusteeIndividualType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("Joe", None, "Bloggs"),
        dateOfBirth = None,
        phoneNumber = None,
        identification = None,
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None,
        entityStart = LocalDate.parse("2012-03-14"),
        entityEnd = None
      )

      val promotedTrustee = AmendedLeadTrusteeIndType(
        name = NameType("Joe", None, "Bloggs"),
        dateOfBirth = LocalDate.parse("1980-03-30"),
        phoneNumber = "tel",
        email = None,
        identification = IdentificationType(None, None, None, None),
        countryOfResidence = None,
        nationality = None,
        legallyIncapable = None
      )

      val trusteeType: String = "trusteeInd"

      "must add a new promote transform" - {

        "4mld" in {

          val mockTransformationService = mock[TransformationService]
          val mockLocalDateService = mock[LocalDateService]

          val controller = new PromoteTrusteeController(
            identifierAction,
            mockTransformationService,
            mockLocalDateService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
            .thenReturn(Future.successful(buildInputJson(
              Seq(Json.toJson(originalTrustee)),
              isTaxable = None
            )))

          when(mockTransformationService.addNewTransform(any(), any(), any()))
            .thenReturn(Future.successful(true))

          when(mockLocalDateService.now).thenReturn(endDate)

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(promotedTrustee))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.promote(utr, index).apply(request)

          status(result) mustBe OK

          val transform = PromoteTrusteeTransform(Some(index), Json.toJson(promotedTrustee), Json.toJson(originalTrustee), endDate, trusteeType, isTaxable = true)

          verify(mockTransformationService)
            .addNewTransform(equalTo(utr), any(), equalTo(transform))
        }

        "5mld" - {

          "taxable" in {

            val mockTransformationService = mock[TransformationService]
            val mockLocalDateService = mock[LocalDateService]

            val controller = new PromoteTrusteeController(
              identifierAction,
              mockTransformationService,
              mockLocalDateService
            )(Implicits.global, Helpers.stubControllerComponents())

            when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
              .thenReturn(Future.successful(buildInputJson(
                Seq(Json.toJson(originalTrustee)),
                isTaxable = Some(true)
              )))

            when(mockTransformationService.addNewTransform(any(), any(), any()))
              .thenReturn(Future.successful(true))

            when(mockLocalDateService.now).thenReturn(endDate)

            val request = FakeRequest(POST, "path")
              .withBody(Json.toJson(promotedTrustee))
              .withHeaders(CONTENT_TYPE -> "application/json")

            val result = controller.promote(utr, index).apply(request)

            status(result) mustBe OK

            val transform = PromoteTrusteeTransform(Some(index), Json.toJson(promotedTrustee), Json.toJson(originalTrustee), endDate, trusteeType, isTaxable = true)

            verify(mockTransformationService)
              .addNewTransform(equalTo(utr), any(), equalTo(transform))
          }

          "non-taxable" in {

            val mockTransformationService = mock[TransformationService]
            val mockLocalDateService = mock[LocalDateService]

            val controller = new PromoteTrusteeController(
              identifierAction,
              mockTransformationService,
              mockLocalDateService
            )(Implicits.global, Helpers.stubControllerComponents())

            when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
              .thenReturn(Future.successful(buildInputJson(
                Seq(Json.toJson(originalTrustee)),
                isTaxable = Some(false)
              )))

            when(mockTransformationService.addNewTransform(any(), any(), any()))
              .thenReturn(Future.successful(true))

            when(mockLocalDateService.now).thenReturn(endDate)

            val request = FakeRequest(POST, "path")
              .withBody(Json.toJson(promotedTrustee))
              .withHeaders(CONTENT_TYPE -> "application/json")

            val result = controller.promote(utr, index).apply(request)

            status(result) mustBe OK

            val transform = PromoteTrusteeTransform(Some(index), Json.toJson(promotedTrustee), Json.toJson(originalTrustee), endDate, trusteeType, isTaxable = false)

            verify(mockTransformationService)
              .addNewTransform(equalTo(utr), any(), equalTo(transform))
          }
        }
      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new PromoteTrusteeController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.promote(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "business trustee" - {

      val originalTrustee = TrusteeOrgType(
        lineNo = None,
        bpMatchStatus = None,
        name = "Name",
        phoneNumber = None,
        email = None,
        identification = None,
        countryOfResidence = None,
        entityStart = LocalDate.parse("2012-03-14"),
        entityEnd = None
      )

      val promotedTrustee = AmendedLeadTrusteeOrgType(
        name = "Name",
        phoneNumber = "tel",
        email = None,
        identification = IdentificationOrgType(None, None, None),
        countryOfResidence = None
      )

      val trusteeType: String = "trusteeOrg"

      "must add a new promote transform" - {

        "4mld" in {

          val mockTransformationService = mock[TransformationService]
          val mockLocalDateService = mock[LocalDateService]

          val controller = new PromoteTrusteeController(
            identifierAction,
            mockTransformationService,
            mockLocalDateService
          )(Implicits.global, Helpers.stubControllerComponents())

          when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
            .thenReturn(Future.successful(buildInputJson(
              Seq(Json.toJson(originalTrustee)),
              isTaxable = None
            )))

          when(mockTransformationService.addNewTransform(any(), any(), any()))
            .thenReturn(Future.successful(true))

          when(mockLocalDateService.now).thenReturn(endDate)

          val request = FakeRequest(POST, "path")
            .withBody(Json.toJson(promotedTrustee))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val result = controller.promote(utr, index).apply(request)

          status(result) mustBe OK

          val transform = PromoteTrusteeTransform(Some(index), Json.toJson(promotedTrustee), Json.toJson(originalTrustee), endDate, trusteeType, isTaxable = true)

          verify(mockTransformationService)
            .addNewTransform(equalTo(utr), any(), equalTo(transform))
        }

        "5mld" - {

          "taxable" in {

            val mockTransformationService = mock[TransformationService]
            val mockLocalDateService = mock[LocalDateService]

            val controller = new PromoteTrusteeController(
              identifierAction,
              mockTransformationService,
              mockLocalDateService
            )(Implicits.global, Helpers.stubControllerComponents())

            when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
              .thenReturn(Future.successful(buildInputJson(
                Seq(Json.toJson(originalTrustee)),
                isTaxable = Some(true)
              )))

            when(mockTransformationService.addNewTransform(any(), any(), any()))
              .thenReturn(Future.successful(true))

            when(mockLocalDateService.now).thenReturn(endDate)

            val request = FakeRequest(POST, "path")
              .withBody(Json.toJson(promotedTrustee))
              .withHeaders(CONTENT_TYPE -> "application/json")

            val result = controller.promote(utr, index).apply(request)

            status(result) mustBe OK

            val transform = PromoteTrusteeTransform(Some(index), Json.toJson(promotedTrustee), Json.toJson(originalTrustee), endDate, trusteeType, isTaxable = true)

            verify(mockTransformationService)
              .addNewTransform(equalTo(utr), any(), equalTo(transform))
          }

          "non-taxable" in {

            val mockTransformationService = mock[TransformationService]
            val mockLocalDateService = mock[LocalDateService]

            val controller = new PromoteTrusteeController(
              identifierAction,
              mockTransformationService,
              mockLocalDateService
            )(Implicits.global, Helpers.stubControllerComponents())

            when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
              .thenReturn(Future.successful(buildInputJson(
                Seq(Json.toJson(originalTrustee)),
                isTaxable = Some(false)
              )))

            when(mockTransformationService.addNewTransform(any(), any(), any()))
              .thenReturn(Future.successful(true))

            when(mockLocalDateService.now).thenReturn(endDate)

            val request = FakeRequest(POST, "path")
              .withBody(Json.toJson(promotedTrustee))
              .withHeaders(CONTENT_TYPE -> "application/json")

            val result = controller.promote(utr, index).apply(request)

            status(result) mustBe OK

            val transform = PromoteTrusteeTransform(Some(index), Json.toJson(promotedTrustee), Json.toJson(originalTrustee), endDate, trusteeType, isTaxable = false)

            verify(mockTransformationService)
              .addNewTransform(equalTo(utr), any(), equalTo(transform))
          }
        }
      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]
        val mockLocalDateService = mock[LocalDateService]

        val controller = new PromoteTrusteeController(
          identifierAction,
          mockTransformationService,
          mockLocalDateService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.promote(utr, index).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }
  }
}
