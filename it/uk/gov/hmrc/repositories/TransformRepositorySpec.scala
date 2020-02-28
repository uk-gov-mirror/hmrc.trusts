package uk.gov.hmrc.repositories

import org.joda.time.DateTime
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import reactivemongo.api.MongoConnection
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, DisplayTrustTrusteeIndividualType}
import uk.gov.hmrc.trusts.repositories.{TransformationRepository, TrustsMongoDriver}
import uk.gov.hmrc.trusts.transformers.{AddTrusteeIndTransform, ComposedDeltaTransform, AmendLeadTrusteeIndTransform}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class TransformRepositorySpec extends FreeSpec with MustMatchers with ScalaFutures with IntegrationPatience {
  private val connectionString = "mongodb://localhost:27017/transform-integration"

  "a transform repository" - {

    "must be able to store and retrieve a payload" in {

      val application = appBuilder.build()

      running(application) {
        getConnection(application).map { connection =>

          dropTheDatabase(connection)

          val repository = application.injector.instanceOf[TransformationRepository]

          val storedOk = repository.set("UTRUTRUTR", "InternalId", data)
          storedOk.futureValue mustBe true

          val retrieved = repository.get("UTRUTRUTR", "InternalId")
            .map(_.getOrElse(fail("The record was not found in the database")))

          retrieved.futureValue mustBe data

          dropTheDatabase(connection)
        }.get
      }
    }
  }

  private lazy val appBuilder = new GuiceApplicationBuilder().configure(Seq(
    "mongodb.uri" -> connectionString,
    "metrics.enabled" -> false,
    "auditing.enabled" -> false,
    "mongo-async-driver.akka.log-dead-letters" -> 0
  ): _*)

  val data = ComposedDeltaTransform(Seq(AmendLeadTrusteeIndTransform(
    DisplayTrustLeadTrusteeIndType(
      Some(""),
      None,
      NameType("New", Some("lead"), "Trustee"),
      DateTime.parse("2000-01-01"),
      "",
      None,
      DisplayTrustIdentificationType(None, None, None, None),
      Some(DateTime.parse("2010-10-10"))
    )),
    AddTrusteeIndTransform(DisplayTrustTrusteeIndividualType(
      Some("lineNo"),
      Some("bpMatchStatus"),
      NameType("New", None, "Trustee"),
      Some(DateTime.parse("2000-01-01")),
      Some("phoneNumber"),
      Some(DisplayTrustIdentificationType(None, Some("nino"), None, None)),
      DateTime.parse("2010-10-10")
    ))
  )
  )

  private def getDatabase(connection: MongoConnection) = {
    connection.database("transform-integration")
  }

  private def getConnection(application: Application) = {
    val mongoDriver = application.injector.instanceOf[TrustsMongoDriver]
    lazy val connection = for {
      uri <- MongoConnection.parseURI(connectionString)
      connection <- mongoDriver.api.driver.connection(uri, true)
    } yield connection
    connection
  }

  def dropTheDatabase(connection: MongoConnection): Unit = {
    Await.result(getDatabase(connection).flatMap(_.drop()), Duration.Inf)
  }
}
