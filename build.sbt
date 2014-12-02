name := "finapi"

version := "1.0"

organization := "net.x1a0"

scalaVersion := "2.10.4"

val finagleVersion = "6.22.0"

libraryDependencies ++= Seq(
  "com.twitter"   %% "finagle-core"   % finagleVersion,
  "com.twitter"   %% "finagle-http"   % finagleVersion,
  "org.atteo"     %  "evo-inflector"  % "1.2.1",
  "org.json4s"    %% "json4s-core"    % "3.2.11",
  "org.json4s"    %% "json4s-jackson" % "3.2.11",
  "org.scalatest" %% "scalatest"      % "2.2.2" % "test",
  "org.mockito"   %  "mockito-all"    % "1.10.8" % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)
