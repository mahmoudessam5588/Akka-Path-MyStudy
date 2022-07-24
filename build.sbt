name := "AKKAMyStudyPath"

version := "0.1"

scalaVersion := "3.1.2"

val akkaVersion = "2.6.19"
val scalaTestVersion = "3.2.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion,
)
