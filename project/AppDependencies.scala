import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val testScope = "test"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "org.reactivemongo"         %% "play2-reactivemongo"        % "0.18.8-play27",
    "uk.gov.hmrc"               %% "bootstrap-backend-play-27"  % "2.25.0",
    "com.github.java-json-tools" % "json-schema-validator"      % "2.2.14"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"           % "3.0.8",
    "org.pegdown"            % "pegdown"              % "1.6.0",
    "com.github.tomakehurst" % "wiremock-standalone"  % "2.25.1",
    "org.mockito"            % "mockito-all"          % "1.10.19",
    "org.scalatestplus.play" %% "scalatestplus-play"  % "4.0.3",
    "com.typesafe.play"      %% "play-test"           % PlayVersion.current
  ).map(_ % Test)

  val akkaVersion = "2.6.7"
  val akkaHttpVersion = "10.1.12"

  val overrides = Seq(
    "com.typesafe.akka" %% "akka-stream"    % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf"  % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j"     % akkaVersion,
    "com.typesafe.akka" %% "akka-actor"     % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
  )

}