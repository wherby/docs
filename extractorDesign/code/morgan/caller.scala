var morganExtractorMap = Map[Seq[String], (AbstractProcessor, ExtractorConfig)](
  Seq("Trial Balance") -> (new MorganTBProcessor, ReportFundTrialBalanceReportConfig.extractorConfig),
  Seq("Position Appraisal") -> (new MorganPositionProcessor, LongShortPositionAppraisalReportConfig.extractorConfig),
  Seq("Purchases and Sales") -> (new MorganPSRProcessor, ReportPurchaseAndSalesConfig.extractorConfig),
  Seq("Ledger movement") -> (new MorganGLProcessor, ReportPurchaseAndSalesConfig.extractorConfig),
  Seq("Realized Gain Loss") -> (new MorganRGLProcessor, ReportRealizedGainLossReportConfig.extractorConfig),
  Seq("DAILY CASH EXPOSURE") -> (new CashExposureReportProcessor, CashExposureReportConfig.extractorConfig),
  Seq("DIVIDEND INCOME AND EXPENSE") -> (new DividendIncomeAndExpenseReportProcessor, DividendIncomeAndExpenseReportConfig.extractorConfig)
)


def processFiles(request: Request[MultipartFormData[Files.TemporaryFile]], fundEngagement: FundEngagementData, fund: Fund,
                 multifondsUploadHelper: MultifondsUploadHelper,
                 doraServer: DoraServer,
                 fundEngagementReportTypeSelectionRead: FundEngagementReportTypeSelectionRead,
                 fundEngagementReportTypeSelectionWrite: FundEngagementReportTypeSelectionWrite): Result = {
  request.body.file("file").map(file => {
    ...
      if (filenames.last == "xlsx" || filenames.last == "xlsm" || filenames.last == "xls") {
        methodName = "readFileAndExtractToObjectForPeriod"
      } else if (filenames.last == "pdf") {
        methodName = "readPDFFileAndExtractToObject"
      } else {
        methodName = "readCSVFileAndExtractToObject"
      }

      extractFileAndStoreDataInDB(file.filename, fundEngagement, fund, targetFile.getPath, userEmail, methodName,
        multifondsUploadHelper,
        doraServer,
        fundEngagementReportTypeSelectionRead,
        fundEngagementReportTypeSelectionWrite)
    }
  })

}


def extractFileAndStoreDataInDB(content: String, fundEngagement: FundEngagementData, fund: Fund, targetFilePath: String, userEmail: String, methodName: String,
                                multifondsUploadHelper: MultifondsUploadHelper,
                                doraServer: DoraServer,
                                fundEngagementReportTypeSelectionRead: FundEngagementReportTypeSelectionRead,
                                fundEngagementReportTypeSelectionWrite: FundEngagementReportTypeSelectionWrite): Result = {
  //filter the extractorMap with report header
  var someExtractor = morganExtractorMap

    ...
  someExtractor.map((kv) => {
    val periodStart = fund.auditPeriodBegin
    val periodEnd = fund.auditPeriodEnd
    val paras = List(targetFilePath, kv._2._2, fund.name, periodStart, periodEnd)
    val result = doraServer.runProcess(paras, kv._2._1.getClass.getCanonicalName.stripSuffix("$"), methodName)
    val newStorage = Await.result(result, ConstVar.ProcessJobWaitTime seconds)
      ...s

  })


}