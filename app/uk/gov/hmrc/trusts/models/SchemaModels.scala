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

package uk.gov.hmrc.trusts.models


import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
/**
  * DES API Schema - definitions models below
  */

case class MatchData(utr: String,
                     name: String,
                     postCode: Option[String]
                    )

object MatchData {
  implicit val matchDataFormat: Format[MatchData] = Json.format[MatchData]
}

case class Correspondence(abroadIndicator: Boolean,
                          name: String,
                          address: AddressType,
                          phoneNumber: String)

object Correspondence {

  implicit val correspondenceReads: Reads[Correspondence] = (
    (JsPath \ "abroadIndicator").format[Boolean] and
      (JsPath \ "name").format[String] and
      (JsPath \ "address").format[AddressType] and
      (JsPath \ "phoneNumber").format[String]
    ) (Correspondence.apply, unlift(Correspondence.unapply))

  implicit val correspondenceWrites: Writes[Correspondence] = Writes { correspondence =>
    Json.obj(
      "abroadIndicator" -> correspondence.abroadIndicator,
      "name" -> correspondence.name.take(56),
      "address" -> correspondence.address,
      "phoneNumber" -> correspondence.phoneNumber
    )
  }


}

case class YearsReturns(var returns: Option[List[YearReturnType]])

object YearsReturns {
  implicit val yearsReturnsFormat: Format[YearsReturns] = Json.format[YearsReturns]
}

case class Assets(monetary: Option[List[AssetMonetaryAmount]],
                  propertyOrLand: Option[List[PropertyLandType]],
                  shares: Option[List[SharesType]],
                  business: Option[List[BusinessAssetType]],
                  partnerShip: Option[List[PartnershipType]],
                  other: Option[List[OtherAssetType]])

object Assets {
  implicit val assetsFormat: Format[Assets] = Json.format[Assets]
}

case class AssetMonetaryAmount(assetMonetaryAmount: Int)

object AssetMonetaryAmount {
  implicit val assetMonetaryAmountFormat: Format[AssetMonetaryAmount] = Json.format[AssetMonetaryAmount]
}

case class Declaration(name: NameType,
                       address: AddressType)

object Declaration {
  implicit val declarationFormat: Format[Declaration] = Json.format[Declaration]
}

case class Trust(
                  details: TrustDetailsType,
                  entities: TrustEntitiesType,
                  assets: Assets)

object Trust {
  implicit val trustFormat: Format[Trust] = Json.format[Trust]
}

case class TrustEntitiesType(naturalPerson: Option[List[NaturalPersonType]],
                             beneficiary: BeneficiaryType,
                             deceased: Option[WillType],
                             leadTrustees: LeadTrusteeType,
                             trustees: Option[List[TrusteeType]],
                             protectors: Option[ProtectorsType],
                             settlors: Option[Settlors]) // Making Settlors optional although, it is mandatory in DES schema there are instances where the settlors page isn't populated.

object TrustEntitiesType {
  implicit val trustEntitiesTypeFormat: Format[TrustEntitiesType] = Json.format[TrustEntitiesType]
}

case class ProtectorsType(protector: Option[List[Protector]],
                          protectorCompany: Option[List[ProtectorCompany]])

object ProtectorsType {
  implicit val protectorsTypeFormat: Format[ProtectorsType] = Json.format[ProtectorsType]
}

case class Protector(name: NameType,
                     dateOfBirth: DateTime,
                     identification: IdentificationType)

object Protector {
  val dateTimePattern = "yyyy-MM-dd"

  implicit val dateFormat = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )

  implicit val protectorFormat: Format[Protector] = Json.format[Protector]
}

case class ProtectorCompany(name: String,
                            identification: IdentificationOrgType)

object ProtectorCompany {
  implicit val protectorCompanyFormat: Format[ProtectorCompany] = Json.format[ProtectorCompany]
}



case class TrusteeType(trusteeInd : TrusteeIndividualType, trusteeOrg : TrusteeOrgType)
object TrusteeType {
  implicit val trusteeTypeFormat : Format[TrusteeType] = Json.format[TrusteeType]
}

case class TrusteeOrgType(name: String,
                          phoneNumber: String,
                          email: Option[String] = None,
                          identification: IdentificationOrgType)

object TrusteeOrgType {
  implicit val trusteeOrgTypeFormat: Format[TrusteeOrgType] = Json.format[TrusteeOrgType]
}

case class TrusteeIndividualType(name: NameType,
                       dateOfBirth: DateTime,
                       phoneNumber: Option[String],
                       identification: IdentificationType)
object TrusteeIndividualType {
  implicit val trusteeIndividualTypeFormat : Format[TrusteeIndividualType] = Json.format[TrusteeIndividualType]
}

case class Settlors(settlor: Option[List[Settlor]],
                    settlorCompany: Option[List[SettlorCompany]])

object Settlors {
  implicit val settlorsFormat: Format[Settlors] = Json.format[Settlors]
}

case class Settlor(name: NameType,
                   dateOfBirth: DateTime,
                   identification: IdentificationType)

object Settlor {
  val dateTimePattern = "yyyy-MM-dd"

  implicit val dateFormat = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )

  implicit val settlorFormat: Format[Settlor] = Json.format[Settlor]
}

case class SettlorCompany(name: String,
                          companyType: Option[String],
                          companyTime: Option[Boolean],
                          identification: IdentificationOrgType)

object SettlorCompany {
  implicit val settlorCompanyFormat: Format[SettlorCompany] = Json.format[SettlorCompany]
}



case class LeadTrusteeType(
                            trusteeType: String,
                            dateOfBirth: Option[DateTime] = None,
                            phoneNumber: String,
                            email: Option[String] = None,
                            individualName: Option[NameType] = None,
                            individualIdentification: Option[IdentificationType] = None,
                            organisationName: Option[String] = None,
                            organisationIdentification: Option[IdentificationOrgType] = None
                          )
object LeadTrusteeType {

  implicit val leadTrusteeTypeReads: Reads[LeadTrusteeType] = (
    (JsPath \ "trusteeType").format[String] and
      (JsPath \ "dateOfBirth").formatNullable[DateTime] and
      (JsPath \ "phoneNumber").format[String] and
      (JsPath \ "email").formatNullable[String] and
      (JsPath \ "individualName").formatNullable[NameType] and
      (JsPath \ "individualIdentification").formatNullable[IdentificationType] and
      (JsPath \ "organisationName").formatNullable[String] and
      (JsPath \ "organisationIdentification").formatNullable[IdentificationOrgType]
    ) (LeadTrusteeType.apply, unlift(LeadTrusteeType.unapply))

  implicit val leadTrusteeTypeWrites: Writes[LeadTrusteeType] = Writes {leadTrustee =>
    val json = leadTrustee.trusteeType match {
      case "Ind" => Json.obj(
        "name" -> leadTrustee.individualName,
        "dateOfBirth" -> leadTrustee.dateOfBirth,
        "identification" -> leadTrustee.individualIdentification,
        "phoneNumber" -> leadTrustee.phoneNumber
      )
      case "Org" => Json.obj(
        "name" -> leadTrustee.organisationName,
        "phoneNumber" -> leadTrustee.phoneNumber,
        "identification" -> leadTrustee.organisationIdentification
      )
    }
    leadTrustee.email match {
      case Some(email) => json.deepMerge(Json.obj(
        "email" -> email
      ))
      case _ => json
    }
  }


  implicit val dateFormat = Format[DateTime]( Reads.jodaDateReads("yyyy-MM-dd"), Writes.jodaDateWrites("yyyy-MM-dd") )

}

case class IdentificationOrgType(utr: Option[String],
                                 address: Option[AddressType])

object IdentificationOrgType {
  implicit val identificationOrgTypeFormat: Format[IdentificationOrgType] = Json.format[IdentificationOrgType]
}

case class BeneficiaryType(individualDetails: Option[List[IndividualDetailsType]],
                           company: Option[List[CompanyType]],
                           trust: Option[List[BeneficiaryTrustType]],
                           charity: Option[List[CharityType]],
                           unidentified: Option[List[UnidentifiedType]],
                           large: Option[List[LargeType]],
                           other: Option[List[OtherType]])

object BeneficiaryType {
  implicit val beneficiaryTypeFormat: Format[BeneficiaryType] = Json.format[BeneficiaryType]
}

case class IndividualDetailsType(name: NameType,
                                 dateOfBirth: DateTime,
                                 vulnerableBeneficiary: Boolean,
                                 beneficiaryType: Option[String],
                                 beneficiaryDiscretion: Option[Boolean],
                                 beneficiaryShareOfIncome: Option[String],
                                 identification: IdentificationType)

object IndividualDetailsType {
  val dateTimePattern = "yyyy-MM-dd"

  implicit val dateFormat = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )

  implicit val individualDetailsTypeFormat: Format[IndividualDetailsType] = Json.format[IndividualDetailsType]
}

case class BeneficiaryTrustType(organisationName: String,
                                beneficiaryDiscretion: Option[Boolean],
                                beneficiaryShareOfIncome: Option[String],
                                identification: TrustBeneficiaryIdentification)

object BeneficiaryTrustType {
  implicit val beneficiaryTrustTypeFormat: Format[BeneficiaryTrustType] = Json.format[BeneficiaryTrustType]
}

case class TrustBeneficiaryIdentification(utr: Option[String],
                                          address: Option[AddressType])

object TrustBeneficiaryIdentification {
  implicit val trustBeneficiaryIdentificationFormat: Format[TrustBeneficiaryIdentification] = Json.format[TrustBeneficiaryIdentification]
}

case class Identification(nino: Option[String],
                          address: Option[AddressType])

object Identification {
  implicit val identificationFormat: Format[Identification] = Json.format[Identification]
}

case class CharityType(organisationName: String,
                       beneficiaryDiscretion: Option[Boolean],
                       beneficiaryShareOfIncome: Option[String],
                       identification: TrustBeneficiaryCharityIdentification)

object CharityType {
  implicit val charityTypeFormat: Format[CharityType] = Json.format[CharityType]
}

case class TrustBeneficiaryCharityIdentification(utr: Option[String],
                                                 address: Option[AddressType])

object TrustBeneficiaryCharityIdentification {
  implicit val trustBeneficiaryCharityIdentificationFormat: Format[TrustBeneficiaryCharityIdentification] = Json.format[TrustBeneficiaryCharityIdentification]
}

case class UnidentifiedType(description: String,
                            beneficiaryDiscretion: Option[Boolean],
                            beneficiaryShareOfIncome: Option[String])

object UnidentifiedType {
  implicit val unidentifiedTypeFormat: Format[UnidentifiedType] = Json.format[UnidentifiedType]
}

case class LargeType(organisationName: String,
                     description: Option[String],
                     numberOfBeneficiary: String,
                     identification: LargeTypeIdentification,
                     beneficiaryDiscretion: Option[Boolean],
                     beneficiaryShareOfIncome: Option[String])

object LargeType {
  implicit val largeTypeFormat: Format[LargeType] = Json.format[LargeType]
}

case class LargeTypeIdentification(utr: Option[String], address: Option[AddressType])

object LargeTypeIdentification {
  implicit val largeTypeIdentificationFormat: Format[LargeTypeIdentification] = Json.format[LargeTypeIdentification]
}

case class OtherType(description: String,
                     address: AddressType,
                     numberOfBeneficiary: Option[String],
                     beneficiaryDiscretion: Option[Boolean],
                     beneficiaryShareOfIncome: Option[String])

object OtherType {
  implicit val otherTypeFormat: Format[OtherType] = Json.format[OtherType]
}

case class CompanyType(organisationName: String,
                       beneficiaryDiscretion: Option[Boolean],
                       beneficiaryShareOfIncome: Option[String],
                       identification: CompanyBeneficiaryIdentification)

object CompanyType {
  implicit val companyTypeFormat: Format[CompanyType] = Json.format[CompanyType]
}

case class CompanyBeneficiaryIdentification(utr: Option[String],
                                            address: Option[AddressType])

object CompanyBeneficiaryIdentification {
  implicit val companyBeneficiaryIdentificationFormat: Format[CompanyBeneficiaryIdentification] = Json.format[CompanyBeneficiaryIdentification]
}

case class NaturalPersonType(name: NameType,
                             dateOfBirth: DateTime,
                             identification: IdentificationType)

object NaturalPersonType {
  val dateTimePattern = "yyyy-MM-dd"

  implicit val dateFormat = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )

  implicit val naturalPersonTypeFormat: Format[NaturalPersonType] = Json.format[NaturalPersonType]
}

case class IdentificationType(nino: Option[String], // OneOf under 'IdentificationType' {0}
                              passport: Option[PassportType], // OneOf under 'IdentificationType' {1}
                              address: Option[AddressType]) // OneOf under 'IdentificationType' {1}

object IdentificationType {
  implicit val identificationTypeFormat: Format[IdentificationType] = Json.format[IdentificationType]
}

case class TrustDetailsType(startDate: DateTime,
                            lawCountry: Option[String],
                            administrationCountry: Option[String],
                            residentialStatus: Option[ResidentialStatusType],
                            typeOfTrust: String,
                            deedOfVariation: Option[String],
                            interVivos: Option[Boolean],
                            efrbsStartDate: Option[DateTime])

object TrustDetailsType {
  val dateTimePattern = "yyyy-MM-dd"

  implicit val dateFormat = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )

  implicit val trustDetailsTypeFormat: Format[TrustDetailsType] = Json.format[TrustDetailsType]
}

case class ResidentialStatusType(uk: Option[UkType],
                                 nonUK: Option[NonUKType])

object ResidentialStatusType {
  implicit val residentialStatusTypeFormat: Format[ResidentialStatusType] = Json.format[ResidentialStatusType]
}

case class UkType(scottishLaw: Boolean,
                  preOffShore: Option[String])

object UkType {
  implicit val ukTypeFormat: Format[UkType] = Json.format[UkType]
}

case class NonUKType(sch5atcgga92: Boolean,
                     s218ihta84: Option[Boolean],
                     agentS218IHTA84: Option[Boolean],
                     trusteeStatus: Option[String])

object NonUKType {
  implicit val nonUKTypeFormat: Format[NonUKType] = Json.format[NonUKType]
}


case class Estate(entities: EntitiesType,
                  administrationEndDate: Option[DateTime],
                  periodTaxDues: String)

object Estate {
  val dateTimePattern = "yyyy-MM-dd"

  implicit val dateFormat = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )

  implicit val estateFormat: Format[Estate] = Json.format[Estate]
}

case class PropertyLandType(buildingLandName: Option[String],
                            address: Option[AddressType],
                            valueFull: Option[Int],
                            valuePrevious: Option[Int])

object PropertyLandType {
  implicit val propertyLandTypeFormat: Format[PropertyLandType] = Json.format[PropertyLandType]
}

case class BusinessAssetType(orgName: String,
                             utr: Option[String],
                             businessDescription: Option[String],
                             address: Option[AddressType],
                             businessValue: Option[Int])

object BusinessAssetType {
  implicit val businessAssetTypeFormat: Format[BusinessAssetType] = Json.format[BusinessAssetType]
}

case class OtherAssetType(description: String,
                          value: Option[Int])

object OtherAssetType {
  implicit val otherAssetTypeFormat: Format[OtherAssetType] = Json.format[OtherAssetType]
}

case class PartnershipType(utr: Option[String],
                           description: String,
                           partnershipStart: Option[DateTime])

object PartnershipType {
  val dateTimePattern = "yyyy-MM-dd"

  implicit val dateFormat = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )

  implicit val partnershipTypeFormat: Format[PartnershipType] = Json.format[PartnershipType]
}

case class SharesType(numberOfShares: String,
                      orgName: Option[String],
                      utr: Option[String],
                      shareClass: Option[String],
                      typeOfShare: Option[String],
                      value: Option[Int])

object SharesType {
  implicit val sharesTypeFormat: Format[SharesType] = Json.format[SharesType]
}

case class YearReturnType(taxReturnYear: String, taxConsequence: Boolean)

object YearReturnType {
  implicit val yearReturnTypeFormat: Format[YearReturnType] = Json.format[YearReturnType]
}

case class YearsReturnType(returns: Option[Array[YearReturnType]])

object YearsReturnType {
  implicit val yearsReturnTypeFormat: Format[YearsReturnType] = Json.format[YearsReturnType]
}

case class EntitiesType(personalRepresentative: PersonalRepresentative,
                        deceased: WillType)

object EntitiesType {
  implicit val entitiesTypeFormat: Format[EntitiesType] = Json.format[EntitiesType]
}

case class PassportType(number: String,
                        expirationDate: DateTime,
                        countryOfIssue: String)

object PassportType {
  val dateTimePattern = "yyyy-MM-dd"

  implicit val dateFormat = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )

  implicit val passportTypeFormat: Format[PassportType] = Json.format[PassportType]
}

case class NameType(firstName: String,
                    middleName: Option[String],
                    lastName: String)

object NameType {
  implicit val nameTypeFormat: Format[NameType] = Json.format[NameType]
}

case class AddressType(line1: String,
                       line2: String,
                       line3: Option[String],
                       line4: Option[String],
                       postCode: Option[String],
                       country: String)

object AddressType {
  implicit val addressTypeFormat: Format[AddressType] = Json.format[AddressType]
}

case class PersonalRepresentative(name: NameType,
                                  dateOfBirth: DateTime,
                                  identification: IdentificationType,
                                  phoneNumber: Option[String],
                                  email: Option[String])

object PersonalRepresentative {
  val dateTimePattern = "yyyy-MM-dd"

  implicit val dateFormat = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )

  implicit val personalRepresentativeFormat: Format[PersonalRepresentative] = Json.format[PersonalRepresentative]
}

case class WillType(name: NameType,
                    dateOfBirth: DateTime,
                    dateOfDeath: DateTime,
                    identification: Identification) // OneOf under 'indentification'

object WillType {
  val dateTimePattern = "yyyy-MM-dd"

  implicit val dateFormat = Format[DateTime]( Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern) )

  implicit val willTypeFormat: Format[WillType] = Json.format[WillType]
}



case class AgentDetails(arn: String,
                        agentName: String,
                        agentAddress: AgentAddressType,
                        agentTelephoneNumber: String,
                        clientReference: String)
object AgentDetails {
  implicit val agentDetailsFormat: Format[AgentDetails] = Json.format[AgentDetails]
}

case class AgentAddressType(isUkAddress: Option[Boolean] = None,
                            line1: Option[String] = None,
                            line2: Option[String] = None,
                            line3: Option[String] = None,
                            line4: Option[String] = None,
                            postCode: Option[String] = None,
                            country: Option[String] = None)
object AgentAddressType {
  implicit val agentAddressTypeFormat: Format[AgentAddressType] = Json.format[AgentAddressType]
}
