# Cache for slow query

## Issue

AWM has a slow query for  getCompletionRatioByEngagementIds(engagementIds), 

fix:
![Cache for slow query](pic/cacheforslowquery.png)

code:
: @@snip[StatisticsController](code/statisticsController.scala)