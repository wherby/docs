override def lookupByFundEngagementId(fundEngagementId: String): Future[Option[EgaData]] = {
  val f: Future[Option[EgaRow]] =
    db.run(queryByFundEngagementId(fundEngagementId).result.headOption)
  f.map { maybeRow => maybeRow.map(rowToData) }
}
