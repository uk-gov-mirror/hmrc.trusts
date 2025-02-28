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

package uk.gov.hmrc.repositories

import java.time.LocalDateTime

import models.MongoDateTimeFormats
import org.scalatest.{AsyncFreeSpec, MustMatchers}
import play.api.libs.json._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.itbase.IntegrationTestBase
import models.registration.RegistrationSubmissionDraft
import repositories.RegistrationSubmissionRepository

class RegistrationSubmissionRepositorySpec extends AsyncFreeSpec with MustMatchers with IntegrationTestBase with MongoDateTimeFormats {

  // Make sure we use value of LocalDateTime that survives JSON round trip - and isn't expired.
  private val testDateTime: LocalDateTime = Json.toJson(LocalDateTime.now()).as[LocalDateTime]

  private val data1 = Json.obj(
    "field1" -> "value1",
    "field2" -> "value2",
    "theAnswer" -> 42
  )
  private val data2 = Json.obj(
    "field1" -> "valueX",
    "field2" -> "valueY",
    "theAnswer" -> 3.14
  )
  private val data3 = Json.obj(
    "field1" -> "valueA",
    "field2" -> "valueB",
    "theAnswer" -> 6.28
  )

  "the registration submission repository" - {

    "must be able to store and retrieve data" in assertMongoTest(createApplication) { app =>
      val repository = app.injector.instanceOf[RegistrationSubmissionRepository]

      repository.getRecentDrafts("InternalId", Agent).futureValue mustBe Seq.empty[RegistrationSubmissionDraft]

      val state1 = RegistrationSubmissionDraft(
        "draftId1",
        "InternalId",
        testDateTime,
        data1,
        Some("reference1"),
        Some(true)
      )

      repository.setDraft(state1).futureValue mustBe true

      val state2 = RegistrationSubmissionDraft(
        "draftId2",
        "InternalId",
        testDateTime,
        data2,
        Some("reference2"),
        Some(true)
      )

      repository.setDraft(state2).futureValue mustBe true

      val state3 = RegistrationSubmissionDraft(
        "draftId1",
        "InternalId2",
        testDateTime,
        data3,
        None,
        None
      )

      repository.setDraft(state3).futureValue mustBe true

      val state4 = RegistrationSubmissionDraft(
        "draftId3",
        "InternalId",
        testDateTime,
        data2,
        Some("reference3"),
        Some(false)
      )

      repository.setDraft(state4).futureValue mustBe true

      repository.getDraft("draftId1", "InternalId").futureValue mustBe Some(state1)
      repository.getDraft("draftId2", "InternalId").futureValue mustBe Some(state2)
      repository.getDraft("draftId1", "InternalId2").futureValue mustBe Some(state3)
      repository.getDraft("draftId3", "InternalId").futureValue mustBe Some(state4)

      repository.getRecentDrafts("InternalId", Agent).futureValue mustBe Seq(state2, state1)
    }

    "must be able to remove drafts no longer being used" in assertMongoTest(createApplication) { app =>

      val repository = app.injector.instanceOf[RegistrationSubmissionRepository]

      repository.removeDraft("draftId1", "InternalId").futureValue mustBe true

      val state1 = RegistrationSubmissionDraft(
        "draftId1",
        "InternalId",
        testDateTime,
        data1,
        Some("ref1"),
        Some(true)
      )

      repository.setDraft(state1).futureValue mustBe true

      repository.getDraft("draftId1", "InternalId").futureValue mustBe Some(state1)

      repository.removeDraft("draftId1", "InternalId").futureValue mustBe true

      repository.getDraft("draftId1", "InternalId").futureValue mustBe None
    }

    "must be able to store and retrieve more than 20 drafts"  in assertMongoTest(createApplication) { app =>

      val repository = app.injector.instanceOf[RegistrationSubmissionRepository]

      repository.getRecentDrafts("InternalId", Agent).futureValue mustBe Seq.empty[RegistrationSubmissionDraft]

      for (i <- 0 until 50) {
        val state = RegistrationSubmissionDraft(
          s"draftId$i",
          "InternalId",
          testDateTime,
          data1,
          Some("reference1"),
          Some(true)
        )
        repository.setDraft(state).futureValue mustBe true
      }

      repository.getRecentDrafts("InternalId", Agent).futureValue.size mustBe 50
      repository.getRecentDrafts("InternalId", Organisation).futureValue.size mustBe 1
    }
  }
}
