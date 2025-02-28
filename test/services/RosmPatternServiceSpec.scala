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

package services

import base.BaseSpec
import exceptions._
import models.tax_enrolments.{SubscriptionIdResponse, TaxEnrolmentFailure, TaxEnrolmentSuccess}
import org.mockito.Mockito.when

import scala.concurrent.Future

class RosmPatternServiceSpec extends BaseSpec {

  private val mockTrustsService = mock[TrustsService]
  private val mockTaxEnrolmentsService = mock[TaxEnrolmentsService]


  private val SUT = new RosmPatternServiceImpl(mockTrustsService, mockTaxEnrolmentsService)

  ".completeRosmTransaction" should {

    val taxable: Boolean = false
    val trn = "XTRN1234567"

    "return success taxEnrolmentSubscriberResponse " when {
      "successfully sets subscriptionId id in tax enrolments for provided trn." in {
        when(mockTrustsService.getSubscriptionId(trn)).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn)).
          thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.setSubscriptionId(trn, taxable)

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }
      }
    }

    "return InternalServerErrorException " when {
      "des is down and not able to return subscription id." in {
        when(mockTrustsService.getSubscriptionId(trn)).
          thenReturn(Future.failed(InternalServerErrorException("")))
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn)).
          thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.setSubscriptionId(trn, taxable)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return InternalServerErrorException " when {
      "tax enrolment service is down " in {
        when(mockTrustsService.getSubscriptionId(trn)).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn)).
          thenReturn(Future.successful(TaxEnrolmentFailure))

        val futureResult = SUT.setSubscriptionId(trn, taxable)

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentFailure
        }
      }
    }
    "return BadRequestException " when {
      "tax enrolment service does not found provided subscription id." in {
        when(mockTrustsService.getSubscriptionId(trn)).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))
        when(mockTaxEnrolmentsService.setSubscriptionId("123456789", taxable, trn)).
          thenReturn(Future.failed(BadRequestException))

        val futureResult = SUT.setSubscriptionId(trn, taxable)

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }
      }
    }

  }//completeRosmTransaction

}


