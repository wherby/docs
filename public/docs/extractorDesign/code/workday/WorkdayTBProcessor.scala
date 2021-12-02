class WorkdayTBProcessor extends BaseReportProcessor {
  override def extractToObjectForPeriod(
                                         sourceFilePath: String,
                                         frontEndFundName: String,
                                         frontEndPeriodStart: Date,
                                         frontEndPeriodEnd: Date
                                       ): GeneralSingleEGAStorage = {
    val storage = new FundManagerSingleEGAStorage()
    val tableRow = {
      BasicExcelReaderHelper.processFileOnExcelReader(
        sourceFilePath,
        frontEndFundName,
        WorkdayTBReader.setting
      )
    }
    BasicTBProcessor.processFile(
      tableRow,
      frontEndPeriodStart,
      frontEndPeriodEnd,
      "NoUse",
      storage
    )
  }
}
