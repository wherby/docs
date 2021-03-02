
import com.pwc.ds.awm.processor.reportconfigs.extractorschema
import com.pwc.ds.awm.processor.reportconfigs.extractorschema.{ColumnConfig, DataType, TableConfig}

object PositionAppraisalReportConfig {
        var reportName = "Position appraisal report"

        var tableHeaderColumns: Seq[Seq[ColumnConfig]] = Seq(
        Seq(ColumnConfig("Portfolio", -1, -1,span = 1, columnNum = 0, rowNum = 0),
        ColumnConfig("Fund Structure", -1, -1,span = 1, columnNum = 1, rowNum = 0),
        ColumnConfig("Portfolio Currency",-1, -1, span = 1, columnNum = 2, rowNum = 0),
        ColumnConfig("Period End Date",-1, -1, span = 1, columnNum = 3, rowNum = 0, columnType = DataType.Date),
        ColumnConfig("Long/Short",-1, -1, span = 1, columnNum = 4, rowNum = 0),
        ColumnConfig("Custodian",-1, -1, span = 1, columnNum = 5, rowNum = 0),
        ColumnConfig("Sort Key",-1, -1, span = 1, columnNum = 6, rowNum = 0),
        ColumnConfig("Currency",-1, -1, span = 1, columnNum = 7, rowNum = 0),
        ColumnConfig("Investment",-1, -1, span = 1, columnNum = 8, rowNum = 0),
        ColumnConfig("Security ID",-1, -1, span = 1, columnNum = 9, rowNum = 0),
        ColumnConfig("Underlying ID",-1, -1, span = 1, columnNum = 10, rowNum = 0),
        ColumnConfig("Sedol",-1, -1, span = 1, columnNum = 11, rowNum = 0),
        ColumnConfig("Isin",-1, -1, span = 1, columnNum = 12, rowNum = 0),
        ColumnConfig("Cusip",-1, -1, span = 1, columnNum = 13, rowNum = 0),
        ColumnConfig("Ticker",-1, -1, span = 1, columnNum = 14, rowNum = 0),
        ColumnConfig("ALT2",-1, -1, span = 1, columnNum = 15, rowNum = 0),
        ColumnConfig("Quantity",-1, -1, span = 1, columnNum = 16, rowNum = 0, columnType = DataType.Number),
        ColumnConfig("Local Market Price",-1, -1, span = 1, columnNum = 17, rowNum = 0, columnType = DataType.Number),
        ColumnConfig("In Element Currency",-1, -1, span = 1, columnNum = 18, rowNum = 0),
        ColumnConfig("Current Local Cost",-1, -1, span = 1, columnNum = 19, rowNum = 0, columnType = DataType.Number),
        ColumnConfig("Local Market Value",-1, -1, span = 1, columnNum = 20, rowNum = 0, columnType = DataType.Number),
        ColumnConfig("Current Book Cost",-1, -1, span = 1, columnNum = 21, rowNum = 0, columnType = DataType.Number),
        ColumnConfig("Book Market Value",-1, -1, span = 1, columnNum = 22, rowNum = 0, columnType = DataType.Number),
        ColumnConfig("Book Unrealized Gain/Loss",-1, -1, span = 1, columnNum = 23, rowNum = 0, columnType = DataType.Number),
        ColumnConfig("% Invest",-1, -1, span = 1, columnNum = 24, rowNum = 0, columnType = DataType.Number),
        ColumnConfig("% NAV",-1, -1, span = 1, columnNum = 25, rowNum = 0, columnType = DataType.Number),
        ColumnConfig("Transaction Type",-1, -1, span = 1, columnNum = 26, rowNum = 0),
        ColumnConfig("Security Exchange Listing",-1, -1, span = 1, columnNum = 27, rowNum = 0),
        ColumnConfig("Investment Type",-1, -1, span = 1, columnNum = 28, rowNum = 0),
        ColumnConfig("Country",-1, -1, span = 1, columnNum = 29, rowNum = 0),
        ColumnConfig("Issue Date",-1, -1, span = 1, columnNum = 30, rowNum = 0, columnType = DataType.Date),
        ColumnConfig("Client Type",-1, -1, span = 1, columnNum = 31, rowNum = 0),
        ColumnConfig("Client Residency/Country Code",-1, -1, span = 1, columnNum = 32, rowNum = 0),
        ColumnConfig("Security Issuer Country Code",-1, -1, span = 1, columnNum = 33, rowNum = 0),
        ColumnConfig("Exchange Rate",-1, -1, span = 1, columnNum = 34, rowNum = 0, columnType = DataType.Number),
        ColumnConfig("Notional Cost",-1, -1, span = 1, columnNum = 35, rowNum = 0),
        ColumnConfig("Swap Book Price",-1, -1, span = 1, columnNum = 36, rowNum = 0)
        ));

        var extractorConfig = new extractorschema.ExtractorConfig(reportType = reportName,
        tableConfig = TableConfig(tableHeaderLineNumber = 1, tableBodyConfig = tableHeaderColumns, reportHeaderLineNumber = 10, blockSeperatorNumber = 1))
        }
