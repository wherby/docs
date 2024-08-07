

lazy val docs = (project in file("docs")).
  //enablePlugins(ParadoxPlugin).
  enablePlugins(ParadoxMaterialThemePlugin).
  settings(
  name  := "document for doradilla",
  addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.10.6"),
  //version := "0.1.0",
  //paradoxTheme := Some(builtinParadoxTheme("generic")),
  paradoxIllegalLinkPath := raw".*\\.md".r,
//   Compile / paradoxMaterialTheme ~= {
//   _.withCustomJavaScript("assets/ga4.js")
// },
//  Compile / paradoxMaterialTheme ~= {
//    _.withGoogleAnalytics("UA-43080126-2") // Remember to change this!
//  },
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


