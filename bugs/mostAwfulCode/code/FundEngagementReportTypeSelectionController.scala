maybeFund
  .map(fund => {
    val fundStrategy: Option[FundReportStrategy] = {
      fundReportStrategyFactory.getFundReportStrategy(fund.fundAdmin)
    }
    fundStrategy match {
      case Some(strategy) =>
        strategy
          .processFundReport(request, disabledAfterYEReports)(fundEngagement, fund)
      case None => {
        val processResult: Result =
          FundAdminName.withNameWithDefault(fund.fundAdmin) match {
            case HSBC_IMS => {
              processIMSFiles(request, fundEngagement, fund, disabledAfterYEReports)
            }
