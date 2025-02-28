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

package repositories

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json._
import reactivemongo.api.Cursor
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import config.AppConfig
import models.registration.RegistrationSubmissionDraft

import scala.concurrent.{ExecutionContext, Future}

trait RegistrationSubmissionRepository {

  def getDraft(draftId: String, internalId: String): Future[Option[RegistrationSubmissionDraft]]

  def setDraft(uiState: RegistrationSubmissionDraft): Future[Boolean]

  def getRecentDrafts(internalId: String, affinityGroup: AffinityGroup): Future[List[RegistrationSubmissionDraft]]

  def removeDraft(draftId: String, internalId: String): Future[Boolean]
}

class RegistrationSubmissionRepositoryImpl @Inject()(
                                          mongo: MongoDriver,
                                          config: AppConfig
                                        )(implicit ec: ExecutionContext) extends RegistrationSubmissionRepository with Logging {

  private val collectionName: String = "registration-submissions"

  private val cacheTtl = config.registrationTtlInSeconds

  private def collection: Future[JSONCollection] =
    for {
      _ <- ensureIndexes
      res <- mongo.api.database.map(_.collection[JSONCollection](collectionName))
    } yield res

  private val createdAtIndex = Index(
    key = Seq("createdAt" -> IndexType.Ascending),
    name = Some("ui-state-created-at-index"),
    options = BSONDocument("expireAfterSeconds" -> cacheTtl)
  )

  private val draftIdIndex = Index(
    key = Seq("draftId" -> IndexType.Ascending),
    name = Some("draft-id-index")
  )

  private val internalIdIndex = Index(
    key = Seq("internalId" -> IndexType.Ascending),
    name = Some("internal-id-index")
  )

  private lazy val ensureIndexes = {
    logger.info("Ensuring collection indexes")
    for {
      collection              <- mongo.api.database.map(_.collection[JSONCollection](collectionName))
      createdCreatedIndex <- collection.indexesManager.ensure(createdAtIndex)
      createdIdIndex          <- collection.indexesManager.ensure(draftIdIndex)
      createdInternalIdIndex  <- collection.indexesManager.ensure(internalIdIndex)
    } yield createdCreatedIndex && createdIdIndex && createdInternalIdIndex
  }

  override def setDraft(uiState: RegistrationSubmissionDraft): Future[Boolean] = {

    val selector = Json.obj(
      "draftId" -> uiState.draftId,
      "internalId" -> uiState.internalId
    )

    val modifier = Json.obj(
      "$set" -> uiState
    )

    collection.flatMap {
      _.update(ordered = false).one(selector, modifier, upsert = true).map {
        lastError =>
          lastError.ok
      }
    }
  }

  override def getDraft(draftId: String, internalId: String): Future[Option[RegistrationSubmissionDraft]] = {
    val selector = Json.obj(
      "draftId" -> draftId,
      "internalId" -> internalId
    )

    collection.flatMap(_.find(
      selector = selector, None).one[RegistrationSubmissionDraft])
  }

  override def getRecentDrafts(internalId: String, affinityGroup: AffinityGroup): Future[List[RegistrationSubmissionDraft]] = {
    val maxDocs = if (affinityGroup == Organisation) 1 else -1

    val selector = Json.obj(
      "internalId" -> internalId,
      "inProgress" -> Json.obj("$eq" -> true)
    )

    collection.flatMap(_.find(
      selector = selector, projection = None)
            .sort(Json.obj("createdAt" -> -1))
            .cursor[RegistrationSubmissionDraft]()
            .collect[List](maxDocs, Cursor.FailOnError[List[RegistrationSubmissionDraft]]()))
  }

  override def removeDraft(draftId: String, internalId: String): Future[Boolean] = {
    val selector = Json.obj(
      "draftId" -> draftId,
      "internalId" -> internalId
    )

    collection.flatMap(_.delete().one(selector)).map(_.ok)
  }

}
