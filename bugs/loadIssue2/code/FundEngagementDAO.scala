override def queryByEngagementId(
                                  engagementId: String
                                ): Future[Seq[FundEngagementData]] = {
  val f: Future[Seq[FundEngagementRow]] =
    db.run(
      FundEngagement
        .filter(x => x.engagementid === engagementId)
        .result
    )
  f.map { maybeRows => maybeRows.map(item => rowToData(item)) }
}