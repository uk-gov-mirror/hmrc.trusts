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

package controllers.transformations.beneficiaries

import controllers.actions.FakeIdentifierAction
import models.NameType
import models.variation._
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import services.TransformationService
import transformers.beneficiaries.AddBeneficiaryTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class AddBeneficiaryControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers
 with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String = "utr"
  private val startDate: LocalDate = LocalDate.parse("2021-01-01")

  private val invalidBody: JsValue = Json.parse("{}")

  "Add beneficiary controller" - {

    "unidentified beneficiary" - {

      val beneficiary = UnidentifiedType(
        lineNo = None,
        bpMatchStatus = None,
        description = "Description",
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        entityStart = startDate,
        entityEnd = None
      )

      val beneficiaryType: String = "unidentified"

      "must add a new add transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(Json.obj()))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(beneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addUnidentified(utr).apply(request)

        status(result) mustBe OK

        val transform = AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addUnidentified(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "individual beneficiary" - {

      val beneficiary = IndividualDetailsType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("Joe", None, "Bloggs"),
        dateOfBirth = None,
        vulnerableBeneficiary = None,
        beneficiaryType = None,
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        identification = None,
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None,
        entityStart = startDate,
        entityEnd = None
      )

      val beneficiaryType: String = "individualDetails"

      "must add a new add transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(Json.obj()))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(beneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addIndividual(utr).apply(request)

        status(result) mustBe OK

        val transform = AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addIndividual(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "charity beneficiary" - {

      val beneficiary = BeneficiaryCharityType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Name",
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        identification = None,
        countryOfResidence = None,
        entityStart = startDate,
        entityEnd = None
      )

      val beneficiaryType: String = "charity"

      "must add a new add transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(Json.obj()))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(beneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addCharity(utr).apply(request)

        status(result) mustBe OK

        val transform = AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addCharity(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "other beneficiary" - {

      val beneficiary = OtherType(
        lineNo = None,
        bpMatchStatus = None,
        description = "Description",
        address = None,
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        countryOfResidence = None,
        entityStart = startDate,
        entityEnd = None
      )

      val beneficiaryType: String = "other"

      "must add a new add transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(Json.obj()))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(beneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addOther(utr).apply(request)

        status(result) mustBe OK

        val transform = AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addOther(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "company beneficiary" - {

      val beneficiary = BeneficiaryCompanyType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Name",
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        identification = None,
        countryOfResidence = None,
        entityStart = startDate,
        entityEnd = None
      )

      val beneficiaryType: String = "company"

      "must add a new add transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(Json.obj()))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(beneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addCompany(utr).apply(request)

        status(result) mustBe OK

        val transform = AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addCompany(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "trust beneficiary" - {

      val beneficiary = BeneficiaryTrustType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Name",
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        identification = None,
        countryOfResidence = None,
        entityStart = startDate,
        entityEnd = None
      )

      val beneficiaryType: String = "trust"

      "must add a new add transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(Json.obj()))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(beneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addTrust(utr).apply(request)

        status(result) mustBe OK

        val transform = AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addTrust(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }

    "large beneficiary" - {

      val beneficiary = LargeType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Name",
        description = "Description",
        description1 = None,
        description2 = None,
        description3 = None,
        description4 = None,
        numberOfBeneficiary = "501",
        identification = None,
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        countryOfResidence = None,
        entityStart = startDate,
        entityEnd = None
      )

      val beneficiaryType: String = "large"

      "must add a new add transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any())(any()))
          .thenReturn(Future.successful(Json.obj()))

        when(mockTransformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(beneficiary))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addLarge(utr).apply(request)

        status(result) mustBe OK

        val transform = AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))

      }

      "must return an error for invalid json" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new AddBeneficiaryController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        val request = FakeRequest(POST, "path")
          .withBody(invalidBody)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addLarge(utr).apply(request)

        status(result) mustBe BAD_REQUEST

      }
    }
  }
}
