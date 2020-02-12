# Database null field error

@@@ index

* [Code](./code/index.md)

 
@@@

## User bug report

User report error when they want to define report types for fund in engagemnt:

![User report](./userreport.png)

When check the request in the network:

![Request](./requestpayload.png)


Compare with valid request in network:

![Valid request](./validRequest.png)


We will find that id is missing in the payload.



## Database check

Check the database:

![Database query](./databasequery.png)


It seems the fund_admin_id field is null, we fix this issue, then user could define report 
types now. But we check the production database, we will find the field will be set to null 
by application:

@@snip [sql.txt](/docs/src/main/paradox/fieldnull/sql.txt)


@@include[mysql code](./code/index.md)