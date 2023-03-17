override def update(fund: Fund): Future[Int] = {
  fundsDAO.lookup(fund.id).flatMap(maybeFund => {
    maybeFund match {

      //if fundAdmin changed, should update fund admin id and remove fund selection types
      case Some(existingFund) if(existingFund.fundAdmin != fund.fundAdmin) => {
        fundAdminDAO.queryByFundAdmin(fund.fundAdmin).flatMap(optionFundAdmin => {
          optionFundAdmin match {
            case Some(fundAdmin) => {
              fund.fundAdminId = Some(fundAdmin.id)
              fundsDAO.update(fund).flatMap(updateResult => {
                fundEngagementDAO.queryByFundId(fund.id).flatMap(engagements => {
                  fundEngagementReportTypeSelectionDAO.deleteByEngagementIds(engagements.map(_.id))
                })
              })
            }
            case _ =>{
              throw new Exception(s"fund admin doesn't exist")
            }
          }
        })
      }
      //if fund audit period or accurency changed, should clear uploaded reports and ega.
      case Some(exisitingFund) if(!exisitingFund.auditPeriodBegin.equals(fund.auditPeriodBegin) || !exisitingFund.auditPeriodEnd.equals(fund.auditPeriodEnd) || !exisitingFund.baseCurrency.equals(fund.baseCurrency)) => {
        fundAdminDAO.queryByFundAdmin(fund.fundAdmin).flatMap(optionFundAdmin => {
          optionFundAdmin match {
            case Some(fundAdmin) => {
              fundsDAO.update(fund).flatMap(res => fundEngagementDAO.queryByFundId(fund.id).flatMap(engagements => {
                fundEngagementReportTypeSelectionWrite.clearUploadedReports(engagements.map(_.id)).flatMap(result => {
                  egaWrite.cleanEGAByFundEngagementId(engagements.map(_.id))
                })
              }))
            }
            case _ => {
              throw new Exception(s"fund admin doesn't exist")
            }
          }
        })
      }
      case Some(existingFund) => {
        fundsDAO.update(fund)
      }
      case _ => {
        throw new Exception(s"fund  ${fund.name} doesn't exist")
      }
    }
  })
}