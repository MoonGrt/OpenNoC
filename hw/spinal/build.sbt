ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "org.opennoc"
ThisBuild / version := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "OpenNoCSpinal",
    libraryDependencies += "com.github.spinalhdl" %% "spinalhdl-core" % "1.12.0",
    compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % "1.12.0"),
  )
