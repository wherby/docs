# Traps in Scala

## Operator will bypass type checking

Filter code
: @@snip[Filter code](code/filter1.scala)

Storage definition
: @@snip[Storage definition](code/storage.scala)

How to fix:

Using function to check class type

Fix type check
: @@snip[Function check](code/filterFix.scala)