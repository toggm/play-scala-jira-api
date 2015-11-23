import play.PlayImport.PlayKeys._

name := """play-scala-jira-api"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

resolvers += "oauth" at "http://oauth.googlecode.com/svn/code/maven/"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  specs2 % Test,
  "org.julienrf" %% "play-json-variants" % "1.0.0",
  "commons-codec" % "commons-codec" % "1.6",
  "org.scala-lang.modules" %% "scala-async" % "0.9.2",
  //"net.oauth.core" % "oauth" % "20100527",
  "net.oauth.core" % "oauth-httpclient4" % "20090913",
  "com.adrianhurt" %% "play-bootstrap3" % "0.4.5-P24",
  "org.webjars" % "font-awesome" % "4.3.0-2",
  cache,
  ws
)
