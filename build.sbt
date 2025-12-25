ThisBuild / version := "0.0.0"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / organization := "org.example"

val spinalVersion = "1.12.0"

lazy val root = (project in file(".")).settings(
  name := "OpenPeriph",
  inThisBuild(List(
    organization := "com.github.spinalhdl",
    scalaVersion := "2.12.18",
    version      := "0.0.0"
  )),
  libraryDependencies ++= Seq(
    "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion,
    "com.github.spinalhdl" %% "spinalhdl-lib"  % spinalVersion,
    "com.github.spinalhdl" %% "spinalhdl-sim"  % spinalVersion,
    compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion),
    "org.scalatest" %% "scalatest" % "3.2.17" % Test,
    "org.yaml" % "snakeyaml" % "1.8",
    "com.github.spinalhdl" %% "vexriscv" % "2.0.0"
  )
)

fork := true
