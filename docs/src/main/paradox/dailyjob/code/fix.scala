class ScheduledTasks @Inject()(...
                               statisticslogWrite: StatisticslogWrite)(implicit executionContext: ExecutionContext) {

  if(isScheduledHost){
    actorSystem.scheduler.schedule(initialDelay = 10.seconds + Random.nextInt(100).seconds, interval = 1.day + Random.nextInt(100).seconds) {
      for{
        _ <- statisticslogWrite.takeSnap();
        _ <-fundsWrite.cleanExpiredFund();
        _ <-fundEngagementWrite.cleanExpiredFundEngagement();
        _<-fundEngagementReportTypeSelectionWrite.cleanExpiredTypeSelection();
        _<-egaWrite.cleanExpiredEga()

      }yield ( Logger.apply(this.getClass.toString).info("Daily operation success") )
    }
  }
}
