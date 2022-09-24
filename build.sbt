name := "http4s4tw"

val http4sVersion = "0.23.16"
val slinkyVersion = "0.7.0"

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "3.1.3",
  scalacOptions ++= Seq("-deprecation", "-feature"),
)

lazy val server = (project in file("server")).settings(commonSettings).settings(
  name := "server",
  libraryDependencies ++= Seq(
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-ember-server" % http4sVersion,
  )
)

lazy val client = (project in file("client")).settings(commonSettings).settings(
  name := "client",
  scalaJSUseMainModuleInitializer := true,
  Compile / fastOptJS / scalaJSLinkerConfig ~= {
    _.withSourceMap(false)
  },
  Compile / npmDependencies ++= Seq(
    "react" -> "16.13.1",
    "react-dom" -> "16.13.1",
  ),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.3.14",
    "org.http4s" %%% "http4s-dom" % "0.2.3",
    "org.http4s" %%% "http4s-client" % http4sVersion,
    "org.http4s" %%% "http4s-circe" % http4sVersion,
    "io.circe" %%% "circe-generic" % "0.14.2",
    "com.armanbilge" %%% "calico" % "0.1.1",
    "me.shadaj" %%% "slinky-core" % slinkyVersion,
    "me.shadaj" %%% "slinky-web" % slinkyVersion,
  )
).enablePlugins(
  ScalaJSPlugin,
  ScalaJSBundlerPlugin
)
