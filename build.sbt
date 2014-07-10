name := "HZUtil"

version := "1.6.0"

organization := "org.hirosezouen"

scalaVersion := "2.11.1"

// Actor of Ver2.10.1-> requires to add libraryDependencies explicitly
libraryDependencies <+= scalaVersion { "org.scala-lang" % "scala-actors" % _ }

// Reflect of Ver2.10.1-> requires to add libraryDependencies explicitly
libraryDependencies <+= scalaVersion { "org.scala-lang" % "scala-reflect" % _ }

// for migration from Scala Actor to Akka Actor
libraryDependencies += "org.scala-lang" % "scala-actors-migration_2.11" % "1.1.0"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "ch.qos.logback" % "logback-core" % "1.1.2"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.7"

parallelExecution in Test := false

//scalacOptions += "-deprecation"

scalacOptions += "-feature"

//logLevel := Level.Debug

