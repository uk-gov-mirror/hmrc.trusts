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

package transformers.assets

import play.api.libs.json._
import transformers.{DeltaTransform, JsonOperations}

case class AddAssetTransform(asset: JsValue, override val `type`: String)
  extends AssetTransform with DeltaTransform with JsonOperations {

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    addToList(input, path, asset)
  }

  override val isTaxableMigrationTransform: Boolean = !isNonEeaBusiness
}

object AddAssetTransform {

  val key = "AddAssetTransform"

  implicit val format: Format[AddAssetTransform] = Json.format[AddAssetTransform]
}
