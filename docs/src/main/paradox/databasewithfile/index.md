# Store file in database

## Store file in database:

It's quiet common that applicaiton will use database blob to store long contents input or files.

For our AWM project do the same way, but when we query the file extaction status in home page:

![Query home](pic/query.png)

We just want a simple answer that if the user upload files are processed:

![Net work](pic/network.png)

with query result from backend:

@@ snip[query result](code/queryresult.txt)


so everything looks as expected.


## Issue:

But as we look the database query log in backend, we will find:

@@snip [database query debug](code/logs.txt)

The log shows there will have query for "upload_file_content". This field is 
a blob file:

@@snip [database schema](code/report.sql)


In this table, we store the file content in field "upload_file_content".

## What's happend:

There is an existed api to query user's input:

@@snip [user query](code/query.scala)

The api is as expected.

But when another person implements another api to query the extraction result of user input files,
the person "reuse" the api. Then the issue happens.


## How to resolve:

1. Change the query.

2. Change the database definition.