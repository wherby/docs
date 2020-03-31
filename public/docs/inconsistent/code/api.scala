
class StatisticsReadImpl @Inject()(...
                                   slickUserlogDAO: SlickUserlogDAO)(implicit ec: ExecutionContext) extends StatisticsRead {
  override def getAWMStatistics(): Future[AWMStatistics] = {
      ....
    val from = Await.result(fromFuture,Duration(1,TimeUnit.SECONDS))

    val to = new Timestamp(DateTime.now.getMillis)
    for(userNum <- statisticsDAO.getUserCount();
      ...
        userEgaNumDaily <-slickStatisticsDailyDAO.getEgaCountDaily(from);
        egaNumDaily <-slickStatisticsDailyDAO.getEgaCountDaily(from);
      ...
    )yield AWMStatistics(Seq(
        ....
      StatisticRecord("Number of users with Single EGA downloaded daily",userEgaNumDaily.toString),
      StatisticRecord("Number of Single EGAs created daily",egaNumDaily.toString),
      ...
    )
    )
  }
}
