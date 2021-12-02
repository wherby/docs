
object WorkdayProcessor {
  val extractorMap = Map[Seq[String], (AbstractProcessor, ExtractorConfig)](
    Seq("TB") -> (new WorkdayTBProcessor, ReportPurchaseAndSalesConfig.extractorConfig),
    Seq("GL") -> (new WorkdayGLProcessor, ReportPurchaseAndSalesConfig.extractorConfig)
  )

  val processFiles = BasicProcessFile.processFiles(extractorMap) _

  val getStorage = BasicProcessFile.getStorageBase(new FundManagerSingleEGAStorage) _
}