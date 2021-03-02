case class ExtractorConfig (reportType: String,
                            headerColumns: Seq[Seq[ColumnConfig]] = Seq[Seq[ColumnConfig]](), // report header span config
        tableConfig: TableConfig,
        summaryConfig: Option[TableConfig] = None,
        summarySeparator: String = "********",
        pageLineNumber: Int = 1,
        nextReportType: String = "",
        tableFirstHeaderWithContent: String = ""
        )
