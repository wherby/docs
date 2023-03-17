class ReportProcessor {}

object ReportProcessor extends AbstractProcessor with BaseFileReader {
  def getMemoryOutput(extractorConfig: ExtractorConfig): AbstractReportMemoryOutput = {
    extractorConfig.reportType match {
      case "PURCHASE TRANSACTION REPORT" => PurchaseTransactionReportMemoryOutput
      case "SALES TRANSACTION REPORT"    => SalesTransactionReportMemoryOutput
      case "ACCOUNTS MOVEMENT REPORT"    => AccountsMovementReportMemoryOutput
      case _ => {
        throw UnsupportedReportTypeException(extractorConfig.reportType)
      }
    }
  }

  def getExcelOutput(
                      extractorConfig: ExtractorConfig
                    ): (ExcelOutputConfig, BaseReportExcelOutput) = {
    extractorConfig.reportType match {
      case "PURCHASE TRANSACTION REPORT" =>
        (PurchaseTransactionReportConfig.outputterConfig, PurchaseTransactionReportExcelOutput)
      case "SALES TRANSACTION REPORT" =>
        (SalesTransactionReportConfig.outputterConfig, PurchaseTransactionReportExcelOutput)
      case "ACCOUNTS MOVEMENT REPORT" =>
        (AccountsMovementReportConfig.outputterConfig, PurchaseTransactionReportExcelOutput)
      case "FUND PRICE REPORT" =>
        (FundPriceReportConfig.outputterConfig, PurchaseTransactionReportExcelOutput)
      case _ => {
        throw UnsupportedReportTypeException(extractorConfig.reportType)
      }
    }
  }

  def getExtractors(content: String, extractorConfig: ExtractorConfig): (
    BlockSeparator,
      SanitizerConfig,
      ExtractorConfig,
      BaseReportSanitizer,
      BaseReportExtractor
    ) = {
    extractorConfig.reportType match {
      case "PURCHASE TRANSACTION REPORT" =>
        (
          new BlockSeparatorImpl {},
          PurchaseTransactionReportConfig.sanitizeConfig,
          PurchaseTransactionReportConfig.extractorConfig,
          SalesTransactionReportSanitizer,
          new PurchaseReportExtractor(PurchaseTransactionReportConfig.singleCurrencyBodyRowConfig)
        )
      case "SALES TRANSACTION REPORT" => {
        val extractorConfig =
          if (content.contains("USING EFFECTIVE YIELD AMORTIZATION"))
            SalesTransactionReportAdditionalConfig.extractorConfig
          else SalesTransactionReportConfig.extractorConfig
        (
          new BlockSeparatorImpl {},
          SalesTransactionReportConfig.sanitizeConfig,
          extractorConfig,
          SalesTransactionReportSanitizer,
          new PurchaseReportExtractor(SalesTransactionReportConfig.singleCurrencyBodyRowConfig)
        )
      }
      case "ACCOUNTS MOVEMENT REPORT" =>
        (
          new AccountsMovementReportBlockSeparator(),
          AccountsMovementReportConfig.sanitizeConfig,
          AccountsMovementReportConfig.extractorConfig,
          AccountsMovementReportSanitizer,
          new AccountsMovementReportExtractor()
        )
      case "FUND PRICE REPORT" =>
        (
          new BlockSeparatorImpl {},
          FundPriceReportConfig.sanitizeConfig,
          FundPriceReportConfig.extractorConfig,
          StaticBaseReportSanitizer,
          new BaseReportExtractor()
        )
      case _ => {
        throw UnsupportedReportTypeException(extractorConfig.reportType)
      }
    }
  }

  def readFileAndExtractToSheets(
                                  sourceFilePath: String,
                                  extractorConfig: ExtractorConfig
                                ): Seq[PurchaseReportExtractedSheet] = {
    val source = extractTextFromFile(sourceFilePath)

    val (blockSeparator, sanitizerConfig, targetExtractorConfig, sanitizer, extractor) =
      getExtractors(source, extractorConfig)

    val rawSheets = logTime("split content into sheet") {
      blockSeparator.splitContentIntoSheets(source)
    }

    val lineBreaker = DataSanitizer.detectLinebreaker(source)

    var sheets = logTime("extract all sheets") {
      rawSheets.zipWithIndex.map(v => {
        logTime("extract single sheet") {
          val sheet = v._1

          val (headers, tables, summaries) = logTime("page block time") {
            blockSeparator.getPageBlocks(
              sheet,
              targetExtractorConfig.tableConfig,
              lineBreaker,
              true,
              targetExtractorConfig.pageLineNumber
            )
          }
          val (sanitizedHeader, sanitizedTable, sanitizedSummary) = logTime("sanitizer time") {
            sanitizer.sanitizeBlocks(headers, tables, summaries, sanitizerConfig)
          }
          val (extractedSummary, extractedTable) = logTime("extract report time") {
            extractor.extractSingleReport(
              sanitizedHeader,
              sanitizedTable,
              sanitizedSummary,
              targetExtractorConfig
            )
          }
          PurchaseReportExtractedSheet(
            s"${targetExtractorConfig.reportType} ${v._2}",
            extractedTable,
            extractedSummary,
            Seq()
          )
        }
      })
    }
    sheets
  }

  override def readFileAndExtractToObjectForPeriod(
                                                    sourceFilePath: String,
                                                    extractorConfig: ExtractorConfig,
                                                    fundName: String,
                                                    frontEndPeriodStart: Date,
                                                    frontEndPeriodEnd: Date
                                                  ): SingleEGAStorage = {
    val sheets    = readFileAndExtractToSheets(sourceFilePath, extractorConfig)
    val outputter = getMemoryOutput(extractorConfig)
    val newStorage = outputter.extractSheetToObjectForPeriod(
      sheets,
      fundName,
      frontEndPeriodStart,
      frontEndPeriodEnd
    )
    newStorage
  }
}
