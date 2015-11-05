import play.PlayImport.PlayKeys._

name := """play-scala-jira-api"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  specs2 % Test,
  "org.julienrf" %% "play-json-variants" % "1.0.0",
  "commons-codec" % "commons-codec" % "1.6",
  cache,
  ws
)
