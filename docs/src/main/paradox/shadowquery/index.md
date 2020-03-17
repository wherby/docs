# Shadow query

## Shadow query issue

For AWM project, when user change audit period, then updated file will be removed. The UI should be refeshed,
and the "uplpaded audit files" should be "Not Started".

But in our project when a fund has updated file as below:

![FundWithUploadFile](./pic/first.png)

When we change audit period:

![ChangePeriod](./pic/edit.png)

But the "uploaded audit files" status is not changed:

![AfterEdit](./pic/after_edit.png)

We could find in the network, all update are executed before the query, but the query return the wrong result.

But when we refresh the page, everything goes fine.

![AfterRefresh](./pic/afterrefresh.png)




## How to fix

### Db config file

Let see the db config file:

@@snip [DB config](./code/db.conf)

We could see the db connection numThreads is 10. When change the 
number to 1.

![AfterDBConfig](./pic/afterdbconfig.png)

Seems the issue is fixed.


### Add some delay to the query

![AddDelay](./pic/adddelay.png)


## Why the issue happens:

Let see what's the update and query code does:

UpdateCode
: @@snip [update](./code/update.scala)

QueryStatus
: @@snip  [QueryStatus](./code/querystatus.scala)