override def listPageWithEngagement(pageNum: Int, pageSize: Int, filterMap: Map[String, String]): Future[Page[(Engageuser,Engagement)]] = {
  val offset = pageSize * (pageNum - 1)
  val engageUsersWithNotDeleted = for{
    (engageusers, engagements) <- Engageusers join Engagements on ((left, right) => left.engagementid === right.id && right.deleted === "false" )
  } yield (engageusers, engagements)

  val defaultFilter = engageUsersWithNotDeleted.filter{ userEngagement =>
    filterMap.get("engagementid").map(someId=>userEngagement._1.engagementid.asColumnOf[String] === someId).getOrElse(true: Rep[Boolean])}
    .filter{userEngagement =>
      filterMap.get("userid").map(someId=>userEngagement._1.userid.asColumnOf[String] like s"%$someId%").getOrElse(true: Rep[Boolean])}
    .filter{userEngagement =>
      filterMap.get("role").map(role => userEngagement._1.role ===role).getOrElse(true: Rep[Boolean])
    }
    .filter(_._1.deleted === "false")

  val query = defaultFilter.sortBy(_._1.createdatetime.desc).drop(offset).take(pageSize)
  for {
    totalRows <- db.run(defaultFilter.length.result)
    result <- db.run(query.result).map(rows => rows.collect { case userEngagement => (engageuserRowToEngageuser(userEngagement._1), toEntity(userEngagement._2))})
  } yield Page(result, pageNum, offset, totalRows)
}