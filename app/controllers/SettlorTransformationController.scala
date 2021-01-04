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

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import controllers.actions.IdentifierAction
import models.variation.{AmendDeceasedSettlor, Settlor, SettlorCompany}
import services.SettlorTransformationService
import transformers.remove.RemoveSettlor
import utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class SettlorTransformationController @Inject()(identify: IdentifierAction,
                                                transformService: SettlorTransformationService)
                                               (implicit val executionContext: ExecutionContext,
                                                  cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def amendIndividualSettlor(identifier: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[Settlor] match {
        case JsSuccess(settlor, _) =>

          transformService.amendIndividualSettlorTransformer(
            identifier,
            index,
            request.internalId,
            settlor
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendIndividualSettlor][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a Settlor - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def addIndividualSettlor(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[Settlor] match {
        case JsSuccess(settlor, _) =>

          transformService.addIndividualSettlorTransformer(
            identifier,
            request.internalId,
            settlor
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addIndividualSettlor][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a Settlor - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def addBusinessSettlor(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[SettlorCompany] match {
        case JsSuccess(companySettlor, _) =>

          transformService.addBusinessSettlorTransformer(
            identifier,
            request.internalId,
            companySettlor
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addBusinessSettlor][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a Settlor - $errors")
          Future.successful(BadRequest)
      }
    }
  }


  def amendBusinessSettlor(identifier: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[SettlorCompany] match {
        case JsSuccess(settlor, _) =>

          transformService.amendBusinessSettlorTransformer(
            identifier,
            index,
            request.internalId,
            settlor
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendBusinessSettlor][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a SettlorCompany - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def removeSettlor(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RemoveSettlor] match {
        case JsSuccess(settlor, _) =>
          transformService.removeSettlor(identifier, request.internalId, settlor) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[removeSettlor][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a RemoveSettlor - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def amendDeceasedSettlor(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[AmendDeceasedSettlor] match {
        case JsSuccess(settlor, _) =>
          transformService.amendDeceasedSettlor(identifier, request.internalId, settlor) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendDeceasedSettlor][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a AmendDeceasedSettlor - $errors")
          Future.successful(BadRequest)
      }
  }

}
