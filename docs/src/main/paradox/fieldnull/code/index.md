## Database define

The database define as below:

Fund
: @@snip [fund.sql](/docs/src/main/paradox/fieldnull/code/fund.sql)

FundAdmin

: @@snip [fundadmin.sql](/docs/src/main/paradox/fieldnull/code/fundadmin.sql)


## Code check

The field fund_admin_id in fund table is queried from fund admin table.

Create fund
: @@snip     [create.scala](./create.scala)

Update fund
: @@snip    [update.scala](./update.scala)


Then we find in update fund, the second branch doesn't retrieve fund_admin_id, so this 
may lead the null field


So we need to fix the issue by add query:

Fix one: by add query for second branch
: @@snip [updatefix1.scala](./updatefix1.scala)

Fix two: by add shared query
: @@snip [updatefix2.scala](./updatefix2.scala)


## After fix

Question 1, what's the root cause of the issue?

1. The Fund table contains redundant information of FundAdmin table

2. The update function has multiple paths, and don't use shared function

3. The shared function should in out of update scope.


Question 2, how to use the single truth principle to resolve issues above? 






