# Daily schedule job


## Issue:

In a project, there is a daily schedule job to do some work, but when we query the database,
the record of daily job is not as expected:

@@ snip [database result](./pic/query.txt)


What's the daily job do:

@@ snip [schedule work](./code/schedule.scala)


How to fix:

@@ snip [fix job](./code/fix.scala)