
//val SomeConfig = config("doradilla")


lazy val docs = (project in file("docs")).
  //enablePlugins(ParadoxPlugin).
  enablePlugins(ParadoxMaterialThemePlugin).
  settings(
  name  := "document for doradilla",
  //version := "0.1.0",
  //paradoxTheme := Some(builtinParadoxTheme("generic")),
  paradoxIllegalLinkPath := raw".*\\.md".r,
 Compile / paradoxMaterialTheme ~= {
   _.withGoogleAnalytics("G-LVLRXPE9ZP") // Remember to change this!
 },
 Compile / paradoxMaterialTheme ~= {
   ParadoxMaterialTheme()
     .withColor("teal", "indigo")
   _.withRepository(uri("https://github.com/wherby/docs"))
     .withSocial(
       uri("https://github.com/wherby"),
       uri("https://wherby.github.io"))
 },
  paradoxProperties in Compile ++= Map("project.description" -> "Description for doradilla library.",
    "github.base_url" -> s"https://github.com/wherby/docs")
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
      "com.digitaltangible" %% "play-guard" % "2.5.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings"
    )
  )
