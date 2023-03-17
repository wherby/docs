val cmds = Seq(
  "sbt flyway/flywayClean",
  "sbt flyway/flywayBaseline",
  "sbt flyway/flywayMigrate"
)