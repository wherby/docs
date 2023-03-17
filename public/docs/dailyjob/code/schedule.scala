class ScheduledTasks @Inject()(...
                               statisticslogWrite: StatisticslogWrite)(implicit executionContext: ExecutionContext) {


  if(isScheduledHost){
    actorSystem.scheduler.schedule(initialDelay = 10.seconds + Random.nextInt(100).seconds, interval = 1.day + Random.nextInt(100).seconds) {
      // the block of code that will be executed
      Await.result( statisticslogWrite.takeSnap(), 10 seconds)
      Await.result(fundsWrite.cleanExpiredFund(), 10 seconds)
      Await.result(fundEngagementWrite.cleanExpiredFundEngagement(), 10 seconds)
      Await.result(fundEngagementReportTypeSelectionWrite.cleanExpiredTypeSelection(), 10 seconds)
      egaWrite.cleanExpiredEga()
    }
  }
}
