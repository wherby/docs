# Pure function design

## Start from simple example

The book [Function Programming in Scala](https://www.manning.com/books/functional-programming-in-scala) start with how to
create pure function methods with example of Cofe shop:

Starter
: @@snip[Code1](./code/cafe1.scala)

With payment interface
: @@snip[Code2](./code/cafe2.scala)

Pure function
: @@snip[Code3](./code/cafe3.scala)


## For AWM project

In AWM project, main data workflow is from input files to EGA(Excel file).

input file => extract content => post extraction actions and write to excel

At first time, "post extraction action and write to excel" are in same step.
As time goes on, there are more requirement on post extraction actions. New post
extraction actions are added to the system.

If keep the same design, the "post extraction action and write to excel" will blow up and more spaghetti code 
shows up. 

input file =>   extract content      => post extraction actions => write to excel

Data type translation:

extract content:       [File]                  => [User Define Content] 

Post extraction action:[User Define Content]   => [User Define Content] 

write to excel:        [User Define Content]   => [Excel File]

There could show the "Post extraction action" which is idempotent operation, which means add
 more "Post extraction action" may impact too much to existed code.



