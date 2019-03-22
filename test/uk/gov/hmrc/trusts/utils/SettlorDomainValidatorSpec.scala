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

package uk.gov.hmrc.trusts.utils

import uk.gov.hmrc.trusts.connectors.BaseSpec
import uk.gov.hmrc.trusts.models.Registration


class SettlorDomainValidatorSpec extends BaseSpec with DataExamples {

  def SUT(registration: Registration) = new SettlorDomainValidation(registration)

  "deceasedSettlorDobIsNotFutureDate" should {
    "return validation error when deceased settlor's date of birth is future date" in {
      val willTrust = willTrustWithValues("2030-01-01","2031-01-01")
      SUT(willTrust).deceasedSettlorDobIsNotFutureDate.get.message mustBe
        "Date of birth must be today or in the past."
      BusinessValidation.check(willTrust).size mustBe 2
    }

    "return None when deceased settlor's date of birth is in past" in {
      val willTrust = willTrustWithValues("2019-01-01","2019-02-01")
      SUT(willTrust).deceasedSettlorDobIsNotFutureDate mustBe None
      BusinessValidation.check(willTrust).size mustBe 0

    }

    "return None when there is no deceased settlor" in {
      val employmentTrust = registrationRequest
      SUT(employmentTrust).deceasedSettlorDobIsNotFutureDate mustBe None
      println(BusinessValidation.check(employmentTrust))
      BusinessValidation.check(employmentTrust).size mustBe 0
    }

    "return validation error when deceased date of death is after date of birth." in {
      val willTrust = willTrustWithValues(deceasedDateOfBirth  ="2016-01-01",deceasedDateOfDeath  ="2015-01-01")
      SUT(willTrust).deceasedSettlorDoDIsNotAfterDob.get.message mustBe "Date of death is after date of birth"

      BusinessValidation.check(willTrust).size mustBe 1
    }

    "return validation error when deceased settlor's date of death is future date" in {
      val willTrust = willTrustWithValues("2019-01-01","2031-01-01")
      SUT(willTrust).deceasedSettlorDoDIsNotFutureDate.get.message mustBe
        "Date of death must be today or in the past."
      BusinessValidation.check(willTrust).size mustBe 1
    }


    "return validation error when deceased settlor nino is same as trustee nino" in {
      val willTrust = willTrustWithValues(deceasedNino="ST123456")
      SUT(willTrust).deceasedSettlorIsNotTrustee.get.message mustBe
        "Deceased NINO is same as trustee NINO."
      BusinessValidation.check(willTrust).size mustBe 1
    }

    "return validation error when settlor is not provided for employment related trust" in {
      val interVivos = willTrustWithValues(typeOfTrust = TypeOfTrust.INTER_VIVOS_SETTLEMENT.toString)
      SUT(interVivos).validateSettlor.get.message mustBe
        "Settlor is mandatory for provided type of trust."
      BusinessValidation.check(interVivos).size mustBe 1
    }


    "return validation error when individual settlor has same NINO" in {
      val employmentTrust = getJsonValueFromString(trustWithValues(settlorNino = "ST019091")).validate[Registration].get
      val response = SUT(employmentTrust).livingSettlorDuplicateNino
      response.flatten.zipWithIndex.map{
        case (error,index) =>
          error.message mustBe "NINO is already used for another individual settlor."
          error.location mustBe s"/trust/entities/settlors/settlor/$index/identification/nino"
      }
      BusinessValidation.check(employmentTrust).size mustBe 1
    }

    "return validation error when individual settlor date of birth is future date." in {
      val employmentTrust = getJsonValueFromString(trustWithValues(settlorDob = "2050-01-01")).validate[Registration].get
      val response = SUT(employmentTrust).livingSettlorDobIsNotFutureDate
      response.flatten.size mustBe 1
      response.flatten.zipWithIndex.map{
        case (error,index) =>
          error.message mustBe "Date of birth must be today or in the past."
          error.location mustBe s"/trust/entities/settlors/settlor/${index+1}/dateOfBirth"
      }
      BusinessValidation.check(employmentTrust).size mustBe 1
    }

    "return validation error when individual settlors has same passport number " in {
      val trust = heritageFundWithValues(settlorPassportNumber = "AB123456789C")
      val response = SUT(trust).livingSettlorDuplicatePassportNumber
      response.flatten.size mustBe 1
      response.flatten.zipWithIndex.map{
        case (error,index) =>
          error.message mustBe "Passport number is already used for another individual settlor."
          error.location mustBe s"/trust/entities/settlors/settlor/$index/identification/passport/number"
      }
      BusinessValidation.check(trust).size mustBe 1
    }

    "return validation error when company settlor has same UTR" in {
      val employmentTrust = getJsonValueFromString(trustWithValues(settlorUtr = "1234561234")).validate[Registration].get
      val response = SUT(employmentTrust).livingSettlorDuplicateUtr
      response.flatten.zipWithIndex.map{
        case (error,index) =>
          error.message mustBe "Utr is already used for another settlor company."
          error.location mustBe s"/trust/entities/settlors/settlorCompany/$index/identification/utr"
      }
      BusinessValidation.check(employmentTrust).size mustBe 1
    }

    "return validation error when company settlor utr is trust utr" in {
      val employmentTrust = getJsonValueFromString(trustWithValues(settlorUtr = "5454541615")).validate[Registration].get
      val response = SUT(employmentTrust).companySettlorUtrIsNotTrustUtr
      response.flatten.size mustBe 1
      response.flatten.zipWithIndex.map {
        case (error,index) =>
          error.message mustBe "Settlor company utr is same as trust utr."
          error.location mustBe s"/trust/entities/settlors/settlorCompany/${index+1}/identification/utr"
      }
      BusinessValidation.check(employmentTrust).size mustBe 1
    }

    "return None when company settlor utr is not trust utr" in {
      val employmentTrust = getJsonValueFromString(trustWithValues(settlorUtr = "5454541616")).validate[Registration].get
      val response = SUT(employmentTrust).companySettlorUtrIsNotTrustUtr
      response.flatten mustBe empty
      BusinessValidation.check(employmentTrust) mustBe empty
    }

  }


}
