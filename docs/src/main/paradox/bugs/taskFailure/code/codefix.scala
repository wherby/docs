  override def cleanFundEngagementExpired(): Future[Int] = {
    val now          = LocalDateTime.now()
    val now30DaysAgo = now.minus(60, ChronoUnit.DAYS)
    val endTimestamp = Timestamp.valueOf(now30DaysAgo)
    val queryByTime = Compiled((endTimestamp: Rep[Timestamp]) =>
      FundEngagementReportTypeSelection
        .filter(_.createdatetime.map { time1 => time1 < endTimestamp }.getOrElse(true))
        .filter(_.selected === true)
        .map(_.fundEngagementId)
    )
    db.run(queryByTime(endTimestamp).result).flatMap { result =>
      Future
        .sequence(result.toSet[String].toSeq.map { res =>
          {
            db.run(queryById(res).delete)
          }
        })
        .flatMap(_ => Future(3))
    }
  }