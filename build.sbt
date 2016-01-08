import sbtrelease._

import ReleaseStateTransformations._

name := "finapi"

organization := "net.x1a0"

val finagleVersion = "6.24.0"

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

releaseSettings

ReleaseKeys.releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion
)
