# Intellij bug for scala  

## Issues

In a if -- else branch, when condition is 0 

then go if branch

![Go 0](./pic/image002.png)

but will also go to else branch 

![Branch else](./pic/image001.png)

In else branch, every variable is lost.

and console log is :

![log](./pic/image005.png)

If changed a bit:
Then go to if branch

![Branch if](./pic/image003.png)

And bypass else:

![By pass else](./pic/image004.png)

and console log is :

![log2](./pic/image006.png)


When go to real case:

If branch

![if branch](./pic/image008.png)

go to else branch

![Else branch](./pic/image007.png)


The code to reproduce:

: @@snip[code to reproduce](./code/DownloadService.scala)


Intellij:2021.2

![Intellij](./pic/image009.png)

Java:OpenJDK 11

![Java](./pic/image010.png)

Scala:2.12.6

![Scala](./pic/image011.png)


## What's happened

The if else block is in the foreach or map funtion, which intellij want to evaluate the return value, then bug happens.