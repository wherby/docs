import java.io.File

val genevaExtractorMap = Map[Seq[String], (AbstractProcessor, ExtractorConfig)](
  Seq("Trial Balance") -> (new TrialBalanceExcelReportProcessor, TrialBalanceReportConfig.extractorConfig),
  Seq("Detailed General Ledger") -> (new DetailedGeneralLedgerExcelReportProcessor, DetailedGeneralLedgerReportConfig.extractorConfig),
  Seq("Position Appraisal Report") -> (new PositionAppraisalReportProcessor, PositionAppraisalReportConfig.extractorConfig),
  Seq("Cash Appraisal Report") -> (new CashAppraisalReportProcessor, CashAppraisalReportConfig.extractorConfig),
  Seq("Purchase and Sale Transaction Report") -> (new PurchaseAndSaleTransactionReportProcessor, PurchaseAndSaleTransactionReportConfig.extractorConfig),
  Seq("Dividends Detail Report") -> (new DividendsDetailReportProcessor, DividendsDetailReportConfig.extractorConfig),
  Seq("Realized Gain Loss Report") -> (new RealisedGainLossReportProcessor, RealisedGainLossReportConfig.extractorConfig),
  Seq("Realized Gain/Loss Report") -> (new RealisedGainLossReportProcessor, RealisedGainLossReportConfig.extractorConfig)
)


def processGenevaFiles(request: Request[MultipartFormData[Files.TemporaryFile]], fundEngagement: FundEngagementData, fund: Fund): Result = {
  request.body.file("file").map(file => {
    ...
    if (filenames.length <= 1) {
      encapErrorResponse(
        play.api.mvc.Results.UnprocessableEntity,
        ExtractionErrorCode.UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION
      )
    } else {
      ....
      if (filenames.last == "xls") {
        methodName = "readFileAndExtractToObjectForPeriod"
      } else {
        methodName = "readCSVFileAndExtractToObject"
      }
      extractGenevaFileAndStoreDataInDB(content, fundEngagement, fund, targetFile.getPath, userEmail, methodName)
    }
  })
    ...
}

def extractGenevaFileAndStoreDataInDB(content: String, fundEngagement: FundEngagementData, fund: Fund, targetFilePath: String, userEmail: String, methodName: String): Result = {
  //filter the extractorMap with report header
  val someExtractor = genevaExtractorMap.find((kv) => {
    kv._1.map(k => content.contains(k)).reduce(_ && _)
  })
  someExtractor.map((kv) => {
    val periodStart = fund.auditPeriodBegin
    val periodEnd = fund.auditPeriodEnd
    val paras = List(targetFilePath, kv._2._2, fund.name, periodStart, periodEnd)
    val result = doraServer.runProcess(paras, kv._2._1.getClass.getCanonicalName.stripSuffix("$"), methodName)
    val newStorage = Await.result(result, ConstVar.ProcessJobWaitTime seconds)
      ....
  })
}