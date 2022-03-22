# Redundant DB operation

## Issue 

The store operation will query database multiple times:
![DB operation](pic/dblog.png)

## Code
When the storage is empty, there is no need to query database

Original code
: @@snip[Original code](code/storeInDB.scala)

Fix code
: @@snip[Fix code](code/storeInDBFix.scala)