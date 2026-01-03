ThisBuild / scalaVersion := "2.12.13"
ThisBuild / version      := "0.0.0"
ThisBuild / organization := "com.github.moongrt"

lazy val root = (project in file("."))
  .settings(
    name := "OpenNoC",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % "3.5.3",
      "edu.berkeley.cs" %% "chiseltest" % "0.5.3" % "test",
      "edu.berkeley.cs" %% "rocketchip" % "1.2.6"
    ),
    scalacOptions ++= Seq(
      "-Xsource:2.11",
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.5.3" cross CrossVersion.full),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
  )
