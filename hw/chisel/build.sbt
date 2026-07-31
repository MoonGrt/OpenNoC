ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "org.opennoc"
ThisBuild / version := "0.1.0"

val chiselVersion = "7.0.0-M2"

lazy val root = (project in file("."))
  .settings(
    name := "OpenNoCChisel",
    libraryDependencies += "org.chipsalliance" %% "chisel" % chiselVersion,
    addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full),
  )
