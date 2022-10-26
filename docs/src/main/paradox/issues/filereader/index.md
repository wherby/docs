# Read Excel will modify

## Issues

When read excel file, the excel file will be modified:

![modified](pic/modifyExcel.png)

change the reader, then the excel will not be modified:

![NoModify](pic/notModify.png)

## Code

Read content

: @@snip[Read excel](code/readFile.scala)

FileIO

: @@snip[File IO](code/FileIO.scala)