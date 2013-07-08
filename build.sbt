name := "HZUtil"

version := "1.4.0"

organization := "org.hirosezouen"

scalaVersion := "2.10.2"

// Actor of Ver2.10.1-> requires to add libraryDependencies explicitly
libraryDependencies <+= scalaVersion { "org.scala-lang" % "scala-actors" % _ }

// Reflect of Ver2.10.1-> requires to add libraryDependencies explicitly
libraryDependencies <+= scalaVersion { "org.scala-lang" % "scala-reflect" % _ }

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.13"

libraryDependencies += "ch.qos.logback" % "logback-core" % "1.0.13"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.5"

parallelExecution in Test := false

scalacOptions += "-deprecation"

scalacOptions += "-feature"

//logLevel := Level.Debug

