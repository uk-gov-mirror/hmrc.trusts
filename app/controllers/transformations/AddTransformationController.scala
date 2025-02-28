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

package controllers.transformations

import controllers.TrustsBaseController
import controllers.actions.IdentifierAction
import controllers.transformations.TransformationHelper.isTrustTaxable
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.TransformationService
import transformers.DeltaTransform

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

abstract class AddTransformationController @Inject()(identify: IdentifierAction,
                                                     transformationService: TransformationService)
                                                    (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends TrustsBaseController(cc) with Logging {

  def transform[T](value: T, `type`: String, isTaxable: Boolean)(implicit wts: Writes[T]): DeltaTransform

  def addNewTransform[T](identifier: String, `type`: String = "")(implicit rds: Reads[T], wts: Writes[T]): Action[JsValue] = {
    identify.async(parse.json) {
      implicit request => {
        request.body.validate[T] match {

          case JsSuccess(entityToAdd, _) =>
            for {
              trust <- transformationService.getTransformedTrustJson(identifier, request.internalId)
              isTaxable <- Future.fromTry(isTrustTaxable(trust))
              _ <- transformationService.addNewTransform(identifier, request.internalId, transform(entityToAdd, `type`, isTaxable))
            } yield {
              Ok
            }

          case JsError(errors) =>
            logger.warn(s"[addNewTransform][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
              s"Supplied json did not pass validation - $errors")
            Future.successful(BadRequest)
        }
      }
    }
  }

}
