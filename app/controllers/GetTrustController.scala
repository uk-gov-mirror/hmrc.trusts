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

package controllers

import controllers.actions.{IdentifierAction, ValidateIdentifierActionProvider}

import javax.inject.Inject
import models.auditing.TrustAuditing
import models.get_trust.GetTrustResponse.CLOSED_REQUEST_STATUS
import models.get_trust.{BadRequestResponse, ResourceNotFoundResponse, _}
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.{AuditService, TransformationService, TrustsService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Constants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetTrustController @Inject()(identify: IdentifierAction,
                                   auditService: AuditService,
                                   trustsService: TrustsService,
                                   transformationService: TransformationService,
                                   validateIdentifier: ValidateIdentifierActionProvider,
                                   cc: ControllerComponents) extends BackendController(cc) with Logging {

  val errorAuditMessages: Map[GetTrustResponse, String] = Map(
    BadRequestResponse -> "Bad Request received from DES.",
    ResourceNotFoundResponse -> "Not Found received from DES.",
    InternalServerErrorResponse -> "Internal Server Error received from DES.",
    ServiceUnavailableResponse -> "Service Unavailable received from DES.",
    ClosedRequestResponse -> "Closed Request response received from DES."
  )

  val errorResponses: Map[GetTrustResponse, Result] = Map (
    ResourceNotFoundResponse -> NotFound,
    ClosedRequestResponse -> Status(CLOSED_REQUEST_STATUS),
    ServiceUnavailableResponse -> ServiceUnavailable
  )

  def getFromEtmp(identifier: String): Action[AnyContent] =
    doGet(identifier, applyTransformations = false, refreshEtmpData = true) {
      result: GetTrustSuccessResponse => Ok(Json.toJson(result))
    }

  def get(identifier: String, applyTransformations: Boolean = false): Action[AnyContent] =
    doGet(identifier, applyTransformations) {
      result: GetTrustSuccessResponse => Ok(Json.toJson(result))
    }

  def getLeadTrustee(identifier: String): Action[AnyContent] =
    doGet(identifier, applyTransformations = true) {
      case processed: TrustProcessedResponse =>
        val pick = (ENTITIES \ LEAD_TRUSTEE).json.pick
        processed.getTrust.transform(pick).fold(
          _ => InternalServerError,
          json => {
            json.validate[DisplayTrustLeadTrusteeType] match {
              case JsSuccess(DisplayTrustLeadTrusteeType(Some(leadTrusteeInd), None), _) =>
                Ok(Json.toJson(leadTrusteeInd))
              case JsSuccess(DisplayTrustLeadTrusteeType(None, Some(leadTrusteeOrg)), _) =>
                Ok(Json.toJson(leadTrusteeOrg))
              case _ =>
                logger.error(s"[getLeadTrustee][UTR/URN: $identifier] something unexpected has happened. " +
                  s"doGet has succeeded but picked lead trustee json has failed validation.")
                InternalServerError
            }
          }
        )
      case _ => Forbidden
    }

  def getTrustDetails(identifier: String, applyTransformations: Boolean): Action[AnyContent] =
    getItemAtPath(identifier, TRUST \ DETAILS, applyTransformations)

  def getYearsReturns(identifier: String): Action[AnyContent] =
    getItemAtPath(identifier, YEARS_RETURNS)

  def getTrustees(identifier: String): Action[AnyContent] =
    getArrayAtPath(identifier, ENTITIES \ TRUSTEES, TRUSTEES)

  def getBeneficiaries(identifier: String): Action[AnyContent] =
    getArrayAtPath(identifier, ENTITIES \ BENEFICIARIES, BENEFICIARIES)

  def getAssets(identifier: String): Action[AnyContent] =
    getArrayAtPath(identifier, TRUST \ ASSETS, ASSETS)

  def getSettlors(identifier: String): Action[AnyContent] =
    processEtmpData(identifier) {
      transformed =>
        val settlorsPath = ENTITIES \ SETTLORS
        val deceasedPath = ENTITIES \ DECEASED_SETTLOR

        val settlors = transformed.transform(settlorsPath.json.pick).getOrElse(Json.obj())
        val deceased = transformed.transform(deceasedPath.json.pick)
        val amendedSettlors = deceased.map {
          deceased => settlors.as[JsObject] + (DECEASED_SETTLOR -> deceased)
        }.getOrElse(settlors)

        Json.obj(SETTLORS -> amendedSettlors)
    }

  def getDeceasedSettlorDeathRecorded(identifier: String): Action[AnyContent] =
    processEtmpData(identifier, applyTransformations = false) {
      etmpData =>
        val deceasedDeathDatePath = ENTITIES \ DECEASED_SETTLOR \ DATE_OF_DEATH
        JsBoolean(etmpData.transform(deceasedDeathDatePath.json.pick).isSuccess)
    }

  private val protectorsPath = ENTITIES \ PROTECTORS

  def getProtectorsAlreadyExist(identifier: String): Action[AnyContent] =
    processEtmpData(identifier) {
      trustData =>
        JsBoolean(!trustData.transform(protectorsPath.json.pick).asOpt.contains(
          Json.obj(INDIVIDUAL_PROTECTOR -> JsArray(), BUSINESS_PROTECTOR -> JsArray()))
        )
    }

  def getProtectors(identifier: String): Action[AnyContent] =
    getArrayAtPath(identifier, protectorsPath, PROTECTORS)

  private val otherIndividualsPath = ENTITIES \ OTHER_INDIVIDUALS

  def getOtherIndividualsAlreadyExist(identifier: String): Action[AnyContent] =
    processEtmpData(identifier) {
      trustData => JsBoolean(trustData.transform((otherIndividualsPath \ 0).json.pick).isSuccess)
    }

  def getOtherIndividuals(identifier: String): Action[AnyContent] =
    getArrayAtPath(identifier, otherIndividualsPath, OTHER_INDIVIDUALS)

  def getNonEeaCompaniesAlreadyExist(identifier: String): Action[AnyContent] = {
    val path: JsPath = TRUST \ ASSETS \ NON_EEA_BUSINESS_ASSET
    processEtmpData(identifier) {
      trustData => JsBoolean(trustData.transform((path \ 0).json.pick).isSuccess)
    }
  }

  def isTrust5mld(identifier: String): Action[AnyContent] = {
    val expressTrustPath = TRUST \ DETAILS \ EXPRESS
    processEtmpData(identifier, applyTransformations = false) { untransformedData =>
      JsBoolean(untransformedData.transform(expressTrustPath.json.pick).isSuccess)
    }
  }

  private def getArrayAtPath(identifier: String, path: JsPath, fieldName: String, applyTransformations: Boolean = true): Action[AnyContent] = {
    getElementAtPath(
      identifier,
      path,
      Json.obj(fieldName -> JsArray()),
      applyTransformations
    ) {
      json => Json.obj(fieldName -> json)
    }
  }

  private def getItemAtPath(identifier: String, path: JsPath, applyTransformations: Boolean = true): Action[AnyContent] = {
    getElementAtPath(
      identifier,
      path,
      Json.obj(),
      applyTransformations
    ) {
      json => json
    }
  }

  private def getElementAtPath(identifier: String, path: JsPath, defaultValue: JsValue, applyTransformations: Boolean)
                              (insertIntoObject: JsValue => JsValue): Action[AnyContent] = {
    processEtmpData(identifier, applyTransformations) {
      transformed => transformed
        .transform(path.json.pick)
        .map(insertIntoObject)
        .getOrElse(defaultValue)
    }
  }

  private def processEtmpData(identifier: String, applyTransformations: Boolean = true)
                             (processObject: JsValue => JsValue): Action[AnyContent] = {
    doGet(identifier, applyTransformations) {
      case processed: TrustProcessedResponse =>
        processed.transform.map {
          case transformed: TrustProcessedResponse =>
            Ok(processObject(transformed.getTrust))
          case _ =>
            InternalServerError
        }.getOrElse(InternalServerError)
      case _ =>
        Forbidden
    }
  }

  private def resetCacheIfRequested(identifier: String, internalId: String, refreshEtmpData: Boolean): Future[Unit] = {
    if (refreshEtmpData) {
      val resetTransforms = transformationService.removeAllTransformations(identifier, internalId)
      val resetCache = trustsService.resetCache(identifier, internalId)
      for {
        _ <- resetTransforms
        cache <- resetCache
      } yield cache
    } else {
      Future.successful(())
    }
  }

  private def doGet(identifier: String, applyTransformations: Boolean, refreshEtmpData: Boolean = false)
                   (f: GetTrustSuccessResponse => Result): Action[AnyContent] = (validateIdentifier(identifier) andThen identify).async {
    implicit request =>
      {
        for {
          _ <- resetCacheIfRequested(identifier, request.internalId, refreshEtmpData)
          data <- if (applyTransformations) {
            transformationService.getTransformedData(identifier, request.internalId)
          } else {
            trustsService.getTrustInfo(identifier, request.internalId)
          }
        } yield (
          successResponse(f, identifier) orElse
            notEnoughDataResponse(identifier) orElse
            errorResponse(identifier)
          ).apply(data)
      } recover {
        case e =>
          logger.error(s"[Session ID: ${request.sessionId}][UTR/URN: $identifier] Failed to get trust info ${e.getMessage}")
          InternalServerError
      }
  }

  private def successResponse(f: GetTrustSuccessResponse => Result,
                              identifier: String)
                             (implicit request: IdentifierRequest[AnyContent]): PartialFunction[GetTrustResponse, Result] = {
    case response: GetTrustSuccessResponse =>
      auditService.audit(
        event = TrustAuditing.GET_TRUST,
        request = Json.obj("utr" -> identifier),
        internalId = request.internalId,
        response = Json.toJson(response)
      )

      f(response)
  }

  private def notEnoughDataResponse(identifier: String)
                                   (implicit request: IdentifierRequest[AnyContent]): PartialFunction[GetTrustResponse, Result] = {
    case NotEnoughDataResponse(json, errors) =>
      val reason = Json.obj(
        "response" -> json,
        "reason" -> "Missing mandatory fields in response received from DES",
        "errors" -> errors
      )

      auditService.audit(
        event = TrustAuditing.GET_TRUST,
        request = Json.obj("utr" -> identifier),
        internalId = request.internalId,
        response = reason
      )

      NoContent
  }

  private def errorResponse(identifier: String)
                           (implicit request: IdentifierRequest[AnyContent]): PartialFunction[GetTrustResponse, Result] = {
    case err =>
      auditService.auditErrorResponse(
        TrustAuditing.GET_TRUST,
        Json.obj("utr" -> identifier),
        request.internalId,
        errorAuditMessages.getOrElse(err, "UNKNOWN")
      )
      errorResponses.getOrElse(err, InternalServerError)
  }
}
