# Notification desgin

## Notification workflow

In PFIC ,external investment notifications will be generated and cleared when data reaches a specific state (as shown in following diagram)

![Notification](pic/external-investment-notification-flow.png)

It's very complex workflow and also not want this notification flow design will
impact the business logic.


## Design code

Controller
: @@snip[Controller](code/CompanyfinancialdataController.scala)

Service
: @@snip[Service](code/CompanyfinancialdataServiceWriteImpl.scala)

WorkflowUtil
: @@snip[WorkflowUtil](code/FinancialDataWorkflowUtil.scala)

DataStructure
: @@snip[Data structure](code/dataStructure.scala)