name := "apiq"

version := "1.0"

scalaVersion := "2.11.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.7",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.7",
  "org.scalaz" %% "scalaz-core" % "7.2.8",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.apache.commons" % "commons-lang3" % "3.5"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked"
)
