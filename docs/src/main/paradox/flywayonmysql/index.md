# Flyway clean failed on mysql8


## Issue

The command as belpw is used for clean database schema:

@@ snip [flyway command](./code/command.scala)
 
 but this command will failed on mysql 8.0 for "sbt flyway/flywayClean" as below:

![clean failed](./pic/cleanfailed.png)

When the database is drop by user and use the same sequence commands, the commands will success (there is no cleanup 
operation because the database is not existed yet). But the commands sequence  will still failed on next round on cleanup 
operation.

While this issue will not occur with mysql 5.7.

## How the issue happens?

Because there is one database migration file defined as below:

@@ snip [create procedure](./code/procedure.sql)


The migration will create two procedures in database but not removed. The procedure will only 
affect mysql 8.0. 