ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

val http4sVersion = "1.0.0-M44"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.19"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test

val AkkaVersion = "2.10.0"
val AkkaHttpVersion = "10.7.0"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
)
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.18"

libraryDependencies += "org.typelevel" %% "cats-effect" % "3.6.1"

val circeVersion = "0.14.12"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

lazy val root = (project in file("."))
  .settings(
    name := "BunkerWeb"
  )
