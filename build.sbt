import play.PlayImport.PlayKeys._

name := """play-scala-jira-api"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.2"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  "org.julienrf" %% "play-json-variants" % "1.0.0",
  "com.github.athieriot" %% "specs2-embedmongo" % "0.7.0" % "test",
  "commons-codec" % "commons-codec" % "1.6",
  cache,
  ws
)
