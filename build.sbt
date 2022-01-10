name := "http4s4tw"

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "3.1.0",
  scalacOptions ++= Seq("-deprecation", "-feature"),
)

lazy val client = (project in file("client")).settings(commonSettings).settings(
  name := "client",
  scalaJSUseMainModuleInitializer := true,
  libraryDependencies ++= Seq(
    "org.typelevel" %%% "cats-effect" % "3.3.4",
    "org.scala-js" %%% "scalajs-dom" % "2.0.0",
  )
).enablePlugins(
  ScalaJSPlugin,
  ScalaJSBundlerPlugin
)
