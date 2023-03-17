override def update(fund: Fund): Future[Int] = {
  val newFundFuture =  fundAdminDAO.queryByFundAdmin(fund.fundAdmin).map{
    tmpOpt =>tmpOpt match{
      case Some(fundAdmin)=>fund.copy(fundAdminId = Some(fundAdmin.id))
      case _=>fund
    }
  }
  val newFund = Await.result(newFundFuture, 1 second)

  fundsDAO.lookup(fund.id).flatMap(maybeFund => {
    maybeFund match {

      //if fundAdmin changed, should update fund admin id and remove fund selection types
      case Some(existingFund) if(existingFund.fundAdmin != fund.fundAdmin) => {
        fundsDAO.update(newFund).flatMap(updateResult => {
          fundEngagementDAO.queryByFundId(fund.id).flatMap(engagements => {
            fundEngagementReportTypeSelectionDAO.deleteByEngagementIds(engagements.map(_.id))
          })
        })
      }
      //if fund audit period or accurency changed, should clear uploaded reports and ega.
      case Some(exisitingFund) if(!exisitingFund.auditPeriodBegin.equals(fund.auditPeriodBegin) || !exisitingFund.auditPeriodEnd.equals(fund.auditPeriodEnd) || !exisitingFund.baseCurrency.equals(fund.baseCurrency)) => {
        fundsDAO.update(newFund).flatMap(res => fundEngagementDAO.queryByFundId(fund.id).flatMap(engagements => {
          fundEngagementReportTypeSelectionWrite.clearUploadedReports(engagements.map(_.id)).flatMap(result => {
            egaWrite.cleanEGAByFundEngagementId(engagements.map(_.id))
          })
        }))
      }
      case Some(existingFund) => {
        fundsDAO.update(newFund)
      }
      case _ => {
        throw new Exception(s"fund  ${fund.name} doesn't exist")
      }
    }
  })
}