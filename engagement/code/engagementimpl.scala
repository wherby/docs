case class EngagementInfo(id: String, name: String, funds: Seq[String])

case class UserEngagementInfo(userRole: String, engagements: Seq[EngagementInfo],funds: Seq[String])


override def verifyForFund(email: String, fundId: String): Future[Boolean] = {
  getUserEngagementInfoByEmail(email).map {
    userEngagement =>
      userEngagement.userRole match {
        case "Admin" => true
        case other if userEngagement.funds.contains(fundId) => true
        case _ => false
      }
  }
}



override def verifyForComapany(email: String, companyId: String, year: Int): Future[Boolean] = {
  getUserEngagementInfoByEmail(email).flatMap {
    userEngagement=>
      userEngagement.userRole match{
        case "Admin" => Future(true)
        case other =>
          val fdIds= fiCompanyInvestmentsDAO.queryByCompanyIdAndYear(companyId,year).map{
            investSeq=>investSeq.map(_.fiId).toSet
          }
          val fdIdsInEng = getUserEngagementInfoByEmail(email).map{
            engs=>engs.funds.toSet
          }
          val comsetF= for(s1 <- fdIds;
                           s2 <- fdIdsInEng)
            yield {
              s1 &s2
            }
          comsetF.map{
            comset=> !comset.isEmpty
          }
      }
  }
}
