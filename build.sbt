


lazy val docs = (project in file("doradilla")).
  enablePlugins(ParadoxPlugin).
  settings(
    name := "document for doradilla",
    paradoxTheme := Some(builtinParadoxTheme("generic"))
  )

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := """document""",
    organization := "io.github.wherby",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings"
    )
  )
