override def create(fund: Fund, engagementId:String): Future[Int] = {
  engagementDAO.lookup(engagementId).flatMap(maybeEngagement => {
    maybeEngagement match {
      case engagement => {
        fundAdminDAO.queryByFundAdmin(fund.fundAdmin).flatMap(optionFundAdmin => {
          optionFundAdmin match {
            case Some(fundAdmin) => {
              fund.fundAdminId = Some(fundAdmin.id)
              fundsDAO.create(fund, engagementId)
            }
            case _ =>{
              throw new Exception(s"fund admin doesn't exist")
            }
          }
        }).flatMap(resFund => {
          var fundEngagement = new FundEngagementData(randomUUID().toString, engagementId, resFund.id, None, resFund.createby, Some(new Timestamp(DateTime.now.getMillis)))
          fundEngagementDAO.create(fundEngagement)
        })
      }
      case _ => {
        throw new Exception(s"engagement with this Id doesn't exist")
      }
    }
  })
}

