# MySQL query failed

## Issue

Error

: @@snip[error log](./txt/error.txt)

## Code 

The query as below

StatDao
: @@snip[SlickStatDAO](./code/SlickStatDAO.scala)

DropEngagementCommandGenerator
: @@snip[DropEngagementCommandGenerator](./code/DropEngagementCommandGenerator.scala)

CreateEngagementCommandGenerator
: @@snip[CreateEngagementCommandGenerator](./code/CreateEngagementCommandGenerator.scala)

The query function is basically create "FEDERATED" table of remote mysql table and do the query.

More about "FEDERATED" storage in the [url](https://dev.mysql.com/doc/refman/8.0/en/federated-storage-engine.html).

For "SQLSTATE(08S01), ErrorCode(1159)" 

The url [MySQL Got timeout reading communication packets when reading federated table](https://dba.stackexchange.com/questions/205398/mysql-got-timeout-reading-communication-packets-when-reading-federated-table) tells some config need to be applied to mysql:

![mysql setting](./pic/stackexchange.png)

## Issue finder in code

After the Mysql config is updated, there will be failure for the job, but no error info printed.

Issue finder start:

Logback setting
: @@snip[Log back setting](code/logback.xml)

Job service impl
: @@snip[Job service impl](code/JobServiceImpl.scala)


## More advance topic

From reference [如何在Scala的for comprehension中使用Future](https://blog.csdn.net/azurelaker/article/details/89604689)

Let's see what's output of each code snip:

s1
: @@snip[SerialFutureMap](../../../../../../app/modules/forComprehension/SerialFutureMap.scala)

p1
: @@snip[ParallelFutureMap](../../../../../../app/modules/forComprehension/ParallelFutureMap.scala)

s2
: @@snip[SerialFutureMap2](../../../../../../app/modules/forComprehension/SerialFutureMap2.scala)

as1
: @@snip[AsyncSerialMap](../../../../../../app/modules/forComprehension/AsyncSerialMap.scala)

ap1
: @@snip[AsyncParallelMap](../../../../../../app/modules/forComprehension/AsyncParallelMap.scala)

p2
: @@snip[ParallelFutureMap2](../../../../../../app/modules/forComprehension/ParallelFutureMap2.scala)

p3
: @@snip[ParallelFutureMap3](../../../../../../app/modules/forComprehension/ParallelFutureMap3.scala)

p4
: @@snip[ParallelFutureMap4](../../../../../../app/modules/forComprehension/ParallelFutureMap4.scala)

p5
: @@snip[ParallelFutureMap5](../../../../../../app/modules/forComprehension/ParallelFutureMap5.scala)


