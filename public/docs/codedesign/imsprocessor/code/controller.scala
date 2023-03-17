val hsbcIMSExtractorMap = Map[Seq[String], (AbstractProcessor, ExtractorConfig)](
  Seq(
    "PURCHASE TRANSACTION REPORT"
  ) -> (ReportProcessor, PurchaseTransactionReportConfig.extractorConfig),
  Seq(
    "QUICK VALUATION REPORT"
  ) -> (QuickValuationReportProcessor, QuickValuationReportConfig.extractorConfig_POSITION),
  Seq(
    "SALES TRANSACTION REPORT"
  ) -> (ReportProcessor, SalesTransactionReportConfig.extractorConfig),
  Seq(
    "ACCOUNTS MOVEMENT REPORT"
  ) -> (ReportProcessor, AccountsMovementReportConfig.extractorConfig),
  Seq(
    "CONSOLIDATED TRIAL BALANCE"
  ) -> (new ConsolidationTrialBalanceReportProcessor, ConsolidatedTrialBalancedReportConfig.extractorConfig),
  Seq(
    "OUTSTANDING DIVIDEND REPORT"
  ) -> (new OutstandingDividendReportProcessor, OutstandingDividendReportConfig.extractorConfig)
)