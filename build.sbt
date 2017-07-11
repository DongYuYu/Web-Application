name := """play-java"""
version := "1.0-SNAPSHOT"
lazy val root = (project in file(".")).enablePlugins(PlayJava)
scalaVersion := "2.11.7"
libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs
)
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.40"
fork in run := false
fork in run := true

fork in run := true