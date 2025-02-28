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

import org.scalatest.{AsyncFreeSpec, MustMatchers}
import repositories.TaxableMigrationRepository
import uk.gov.hmrc.itbase.IntegrationTestBase

class TaxableMigrationRepositorySpec extends AsyncFreeSpec with MustMatchers with IntegrationTestBase {

  "TaxableMigrationRepository" - {

    "must be able to store and retrieve a boolean" in assertMongoTest(createApplication) { application =>

      val repository = application.injector.instanceOf[TaxableMigrationRepository]

      val migratingToTaxable = true

      val storedOk = repository.set("UTRUTRUTR", "InternalId", migratingToTaxable)
      storedOk.futureValue mustBe true

      val retrieved = repository.get("UTRUTRUTR", "InternalId")

      retrieved.futureValue mustBe Some(migratingToTaxable)
    }
  }
}
