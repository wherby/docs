import java.sql.Date

class WorkdayGLProcessor extends BaseReportProcessor {
  override def extractToObjectForPeriod(
                                         sourceFilePath: String,
                                         frontEndFundName: String,
                                         frontEndPeriodStart: Date,
                                         frontEndPeriodEnd: Date
                                       ): GeneralSingleEGAStorage = {
    val storage = new FundManagerSingleEGAStorage()
    val tableRow =
      BasicExcelReaderHelper.processFileOnExcelReader(
        sourceFilePath,
        frontEndFundName,
        WorkdayGLReader.setting
      )

    val currency      = WorkdayGLReader2.getCurrency(sourceFilePath)
    val groupedRecord = WorkdayGLReader2.getGroupedValue(sourceFilePath)
    val tableRow2 = BasicExcelReaderHelper.processFileOnExcelReader(
      sourceFilePath,
      frontEndFundName,
      WorkdayGLReader2.setting(currency, groupedRecord)
    )
    BasicGLProcessor.processFile(
      tableRow ++ tableRow2,
      frontEndPeriodStart,
      frontEndPeriodEnd,
      TranDate,
      storage
    )
  }
}
