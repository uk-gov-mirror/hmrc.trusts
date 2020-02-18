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

package uk.gov.hmrc.trusts.controllers

import javax.inject.Inject
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustLeadTrusteeType, DisplayTrustTrusteeIndividualType, DisplayTrustTrusteeOrgType, DisplayTrustTrusteeType}
import uk.gov.hmrc.trusts.services.TransformationService
import uk.gov.hmrc.trusts.utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class TransformationController @Inject()(
                                  identify: IdentifierAction,
                                  transformationService: TransformationService
                                  )(implicit val executionContext: ExecutionContext) extends TrustsBaseController with ValidationUtil {

  def amendLeadTrustee(utr: String) = identify.async(parse.json) {
    implicit request => {
      request.body.validate[DisplayTrustLeadTrusteeType] match {
        case JsSuccess(model, _) =>
          transformationService.addAmendLeadTrusteeTransformer(utr, request.identifier, model) map { _ =>
            Ok
          }
        case JsError(_) =>
          Future.successful(BadRequest)
      }
    }
  }

  def addTrustee(utr: String) = identify.async(parse.json) {
    implicit request => {

      val trusteeInd = request.body.validateOpt[DisplayTrustTrusteeIndividualType].getOrElse(None)
      val trusteeOrg = request.body.validateOpt[DisplayTrustTrusteeOrgType].getOrElse(None)

      (trusteeInd, trusteeOrg) match {
        case (Some(ind), _) =>
          transformationService.addAddTrusteeTransformer(utr, request.identifier, DisplayTrustTrusteeType(Some(ind), None)) map { _ =>
            Ok
          }
        case (_, Some(org)) =>
          transformationService.addAddTrusteeTransformer(utr, request.identifier, DisplayTrustTrusteeType(None, Some(org))) map { _ =>
            Ok
          }
        case _ =>
          Future.successful(BadRequest)
      }
    }
  }
}
