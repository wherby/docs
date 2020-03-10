# Engagement entity add to database

## User story

In PFIC system, PwC internal user could visit all their business information without restriction at beginning.


@@@ note { title="Business information" }

PFIC system has fund, company information. Fund will invest multiple companies.
So the business information will includes information for funds and companies. 
@@@

But client want to introduce engagement concept to restrict PwC internal user to handle engagement's business information.


@@@ note { title=Engagement }

PwC internal user could be in one or more engagements;
One engagement will have multiple funds;
One funds will invest multiple companies. 
@@@

### Question about privilege in different engagement

One user could be in one or more engagements, but does a same user in different engagements with different roles? 

![Email info](./code/email.png)

## DB design

Database as below:

User
: @@snip [User table schema](./code/user.sql)

Engagement
: @@snip [Engagement schema](./code/engagement.sql)

Fund-company
: @@snip [Fund company schema](./code/fund_company.sql)

Fund
: @@snip [Fund schema](./code/fund.sql)

Company
: @@snip [Comapany schema](./code/company.sql)

### Question

How could we check the user's privilege on company and fund?

@@snip [EngagementReadImpl](./code/engagementimpl.scala)


## Question Two

### 1. If one user has different privilege in different engagement, how to design the database?


### 2. What will happen when user binding to engagement table not engagement binging to user table?