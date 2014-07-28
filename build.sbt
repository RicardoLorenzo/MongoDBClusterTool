import play.Project._

name := """mongodb-google-gce"""

version := "0.1"

scalaVersion := "2.10.4"

playJavaSettings

resolvers += "Maven Repository" at "http://http://mvnrepository.com/artifact/"

libraryDependencies ++= Seq(
  "com.google.api-client" % "google-api-client" % "1.18.0-rc",
  "com.google.api-client" % "google-api-client-jackson2" % "1.18.0-rc",
  "com.google.oauth-client" % "google-oauth-client" % "1.18.0-rc",
  "com.google.apis" % "google-api-services-compute" % "v1-rev27-1.18.0-rc",
  "com.google.apis" % "google-api-services-storage" % "v1-rev6-1.19.0",
  "org.springframework" % "spring-context" % "4.0.6.RELEASE",
  "com.jcraft" % "jsch" % "0.1.51",
  "javax.inject" % "javax.inject" % "1",
  "org.webjars" %% "webjars-play" % "2.2.0",
  "org.webjars" % "bootstrap" % "2.3.2",
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "junit" % "junit" % "4.11" % "test",
  "com.novocode" % "junit-interface" % "0.9" % "test->default"
)

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")
