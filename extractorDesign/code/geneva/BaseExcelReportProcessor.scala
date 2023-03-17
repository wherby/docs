package com.pwc.ds.awm.processor.excelReport

import java.io.{File, FileInputStream, FileOutputStream}
import java.sql.Date
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.TimeZone

import com.pwc.ds.awm.component.safelogger.SafeLogger
import com.pwc.ds.awm.processor.exceptions.ExtractionException
import com.pwc.ds.awm.processor.generateEGA.basicReport.GenevaSingleEGAStorage
import com.pwc.ds.awm.processor.reportconfigs.extractorschema.{BlockConfig, ColumnConfig, ExtractorConfig}
import com.pwc.ds.awm.processor.{BaseReportProcessor, date2UTCLocalDate}
import org.apache.commons.lang.StringUtils
import org.apache.poi.hssf.usermodel.{HSSFDateUtil, HSSFWorkbook}
import org.apache.poi.ss.usermodel.{CellStyle, CellType, DateUtil, Row}
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import play.api.Logger

import scala.util.control.Breaks.{break, breakable}

class BaseExcelReportProcessor extends BaseReportProcessor{

  val excelFormatter = new SimpleDateFormat("MM/dd/yyyy")
  val csvFormatter = new SimpleDateFormat("MM-dd-yyyy")
  val outputFormatter = new SimpleDateFormat("dd/MM/yyyy")

  override def extractToObjectForPeriod (sourceFilePath: String, frontEndFundName: String, frontEndPeriodStart: Date, frontEndPeriodEnd: Date): Any = {

    val inputStream = new FileInputStream(new File(sourceFilePath))
    val workbook = new HSSFWorkbook(inputStream)

    var sheetIterator = workbook.sheetIterator()
    var hasReportFound = false

    var isHeader = true
    var isTableHeader = true

    var newStorage: Any = new GenevaSingleEGAStorage()
    var tableHeaderColumns = reportConfig.tableConfig.tableBodyConfig
    var headerList = tableHeaderColumns(0);
    var headerNum = headerList.length

    var reportHeaders = Seq[String]()
    var tableHeaders = Seq[String]()
    var tablebodies = Seq[BlockConfig]()

    while(sheetIterator.hasNext){
      var sheet = sheetIterator.next()
      var lastIndex = -1
      var rowIterator = sheet.rowIterator()
      while(rowIterator.hasNext){
        var row = rowIterator.next()
        var index = row.getRowNum
        if(index != lastIndex +1){
          isHeader =false
        }
        lastIndex = index
        if(checkIfRowIsEmpty(row)){
          isHeader = false
        }
        if(reportHeaders.contains("Dividends Detail Report:")){
          if(row.getCell(0).getStringCellValue == "Portfolio"){
            isHeader = false
          }
        }
        if(isHeader){
          reportHeaders = reportHeaders :+ getReportHeader(row)
        }else{
          if(isTableHeader){
            var nextRow = rowIterator.next()
            if(!checkIfRowIsEmpty(nextRow)){
              tableHeaders = getTableHeaders(row, headerNum)
              isTableHeader = false
            }else{
              tableHeaders = getTableHeaders(rowIterator.next(), headerNum)
              isTableHeader = false
            }
          }else{
            if(!checkIfRowIsEmpty(row)){
              tablebodies = tablebodies :+ getTableContent(row, headerNum)
            }
          }
        }
      }
      var reportHeadersWithContent = reportHeaders.filter(header => {
        header.trim.length > 0
      })
      var tableHeadersWithContent = tableHeaders.filter(header => {
        header.trim.length > 0
      })
      if(tablebodies.length == 0) {
        throw ExtractionException("File contains 0 records, please check your file and upload again.")
      }
      if(reportHeadersWithContent.contains("Report Type: Receivable Summary:")){
        throw ExtractionException("Please upload Dividends Detail Report in income summary format.")
      }
      if (reportHeadersWithContent.length > 0) {
        hasReportFound = true

        //verify whether the uploaded excel is valid
        import java.text.SimpleDateFormat
        var formatter = new SimpleDateFormat("MM/dd/yyyy")
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
        var excelPeriodStartStrList = Seq[String]()
        var excelPeriodStartDate = ""
        var excelPeriodEndStrList = Seq[String]()
        var excelPeriodEndDate = ""
        var fileStartDate = LocalDate.now()
        var fileEndDate = LocalDate.now()
        breakable{
          for(i <-0 to reportHeadersWithContent.length-1){
            if(reportHeadersWithContent(i).contains("Period Start Date")) {
              excelPeriodStartStrList = reportHeadersWithContent(i).split(":")
              excelPeriodStartDate = excelPeriodStartStrList(1)
              fileStartDate = date2UTCLocalDate(formatter.parse(excelPeriodStartDate))
            }
            if(reportHeadersWithContent(i).contains("Period End Date")){
              excelPeriodEndStrList = reportHeadersWithContent(i).split(":")
              excelPeriodEndDate = excelPeriodEndStrList(1)
              fileEndDate = date2UTCLocalDate(formatter.parse(excelPeriodEndDate))
              break()
            }
          }
        }

        newStorage = outputTableBodyRowIntoObjectForPeriod(tableHeaderColumns, tablebodies, this.lineBreaker, frontEndPeriodStart, frontEndPeriodEnd, fileStartDate, fileEndDate, false)
      }
    }
    if (!hasReportFound) {
      SafeLogger.logString(Logger, s"No report found for fund ${frontEndFundName} in period ${frontEndPeriodStart} ${frontEndPeriodEnd}")
    }
    newStorage
  }

  def getReportHeader(row: Row): String = {
    var reportHeader = ""
    val cellIterator = row.cellIterator()
    while(cellIterator.hasNext){
      var cell = cellIterator.next()
      cell.getCellType match {
        case CellType.NUMERIC => reportHeader = reportHeader + cell.getNumericCellValue + ":"
        case CellType.STRING => reportHeader = reportHeader + cell.getStringCellValue + ":"
        case _ => {}
      }
    }
    reportHeader
  }
  def getTableHeaders(row: Row, headerNum: Int): Seq[String] = {
    var tableHeaders = Seq[String]()

    for(cn <- 0 to headerNum-1) {
      var cell = row.getCell(cn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
      cell.getCellType() match {
        case CellType.STRING => {tableHeaders = tableHeaders :+ cell.getStringCellValue}
        case CellType.BOOLEAN => {tableHeaders = tableHeaders :+ cell.getBooleanCellValue.toString}
        case CellType.NUMERIC => {tableHeaders = tableHeaders :+ cell.getNumericCellValue.toString}
        case CellType.BLANK => {
          if(cn == headerNum-1){
            tableHeaders = tableHeaders :+ "null"
          } else {
            tableHeaders = tableHeaders :+ ""
          }
        }
      }
    }
    tableHeaders
  }

  def getTableContent(row: Row, headerNum: Int): BlockConfig = {
    var rowValues = Seq[String]()

    for(cn <- 0 to headerNum-1) {
      var cell = row.getCell(cn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
      cell.getCellType() match {
        case CellType.STRING => {rowValues = rowValues :+ cell.getStringCellValue}
        case CellType.BOOLEAN => {rowValues = rowValues :+ cell.getBooleanCellValue.toString}
        case CellType.NUMERIC => {
          if (DateUtil.isCellDateFormatted(cell)) {
            rowValues = rowValues :+ excelFormatter.format(cell.getDateCellValue)
          } else {
            rowValues = rowValues :+ cell.getNumericCellValue.toString
          }
        }
        case CellType.BLANK => {
          if(cn == headerNum-1){
            rowValues = rowValues :+ "null"
          } else {
            rowValues = rowValues :+ ""
          }
        }
      }
    }

    BlockConfig(rowValues.mkString("|"))
  }

  def checkIfRowIsEmpty(row: Row): Boolean = {
    if(row == null) {
      return true
    }
    if(row.getLastCellNum <= 0){
      return true
    }
    val cellIterator = row.cellIterator()
    while(cellIterator.hasNext){
      val cell = cellIterator.next()
      if(cell != null && cell.getCellTypeEnum() != CellType.BLANK && StringUtils.isNotBlank(cell.toString())){
        return false
      }
    }
    true
  }

  def outputTableBodyRowIntoObjectForPeriod(tableHeaderColumns: Seq[Seq[ColumnConfig]], bodyBlocks: Seq[BlockConfig], lineBreaker: String, frontEndPeriodStart: Date, frontEndPeriodEnd: Date, fileStartDate: LocalDate, fileEndDate: LocalDate, isCSV: Boolean): Any = {
    new GenevaSingleEGAStorage()
  }

  override def extractToExcel(sourceFilePath: String): XSSFWorkbook = {
    val workbook = new XSSFWorkbook()

    val cellStyle = workbook.createCellStyle
    val createHelper = workbook.getCreationHelper
    cellStyle.setDataFormat(createHelper.createDataFormat.getFormat("MM/dd/yyyy"))

    val inputStream = new FileInputStream(new File(sourceFilePath))
    val workbookSource = new HSSFWorkbook(inputStream)

    var sheetIterator = workbookSource.sheetIterator()

    var isHeader = true
    var isTableHeader = true

    var tableHeaderColumns = reportConfig.tableConfig.tableBodyConfig
    var headerList = tableHeaderColumns(0);
    var headerNum = headerList.length

    var reportHeaders = Seq[String]()
    var tableHeaders = Seq[String]()
    var tablebodies = Seq[BlockConfig]()
    while(sheetIterator.hasNext) {
      var sheet = sheetIterator.next()
      var rowIterator = sheet.rowIterator()
      while (rowIterator.hasNext) {
        var row = rowIterator.next()
        if (checkIfRowIsEmpty(row)) {
          isHeader = false
        }
        if (isHeader) {
          reportHeaders = reportHeaders :+ getReportHeader(row)
        } else {
          if (isTableHeader) {
            var nextRow = rowIterator.next()
            if(!checkIfRowIsEmpty(nextRow)){
              tableHeaders = getTableHeaders(nextRow, headerNum)
              isTableHeader = false
            }else{
              tableHeaders = getTableHeaders(rowIterator.next(), headerNum)
              isTableHeader = false
            }
          } else {
            tablebodies = tablebodies :+ getTableContent(row, headerNum)
          }
        }
      }
      var reportHeadersWithContent = reportHeaders.filter(header => {
        header.trim.length > 0
      })
      var tableHeadersWithContent = tableHeaders.filter(header => {
        header.trim.length > 0
      })
    }

    var reportName = reportConfig.reportType
    var bodySheet = workbook.createSheet(reportName)
    outputExcelTableHeaderRow(bodySheet, tableHeaderColumns)
    outputExcelTableBodyRow(bodySheet, tablebodies, cellStyle)
    workbook
  }

  def outputExcelTableHeaderRow(sheet: XSSFSheet, tableHeaderColumns: Seq[Seq[ColumnConfig]]):Unit = {

    var rowNum = 0
    var row = sheet.createRow(rowNum);

    reportConfig.reportType match {
      case "Trial balance" => {
        row.createCell(0).setCellValue("Group1")
        row.createCell(1).setCellValue("Group2")
        row.createCell(2).setCellValue("FinancialAccount_Detail")
        row.createCell(3).setCellValue("Description_Detail")
        row.createCell(4).setCellValue("OpeningBalance_Detail")
        row.createCell(5).setCellValue("Debits_Detail")
        row.createCell(6).setCellValue("Credits_Detail")
        row.createCell(7).setCellValue("ClosingBalance_Detail")
        row.createCell(8).setCellValue("Account name")
        row.createCell(9).setCellValue("Balance")
      }
      case "Detailed general ledger" => {
        row.createCell(0).setCellValue("BeginingBalanceDescription")
        row.createCell(1).setCellValue("BeginingBalanceAmount")
        row.createCell(2).setCellValue("Tran Date")
        row.createCell(3).setCellValue("Tran ID")
        row.createCell(4).setCellValue("Tran Description")
        row.createCell(5).setCellValue("Investment")
        row.createCell(6).setCellValue("Loc")
        row.createCell(7).setCellValue("Currency")
        row.createCell(8).setCellValue("Local Amount")
        row.createCell(9).setCellValue("Book Amount")
        row.createCell(10).setCellValue("Balance")
        row.createCell(11).setCellValue("EndingBalanceDescription")
        row.createCell(12).setCellValue("EndingBalanceAmount")
      }
      case "Position appraisal report" => {
        row.createCell(0).setCellValue("Group1")
        row.createCell(1).setCellValue("Group2")
        row.createCell(2).setCellValue("InvestID")
        row.createCell(3).setCellValue("Portfolio")
        row.createCell(4).setCellValue("Description")
        row.createCell(5).setCellValue("Quantity")
        row.createCell(6).setCellValue("MarketPrice")
        row.createCell(7).setCellValue("CostBook")
        row.createCell(8).setCellValue("MarketValueLocal")
        row.createCell(9).setCellValue("MarketValueBook")
        row.createCell(10).setCellValue("UnrealizedGainOrLoss")
        row.createCell(11).setCellValue("PercentAssets")
        row.createCell(12).setCellValue("ExtendedDescription")
        row.createCell(13).setCellValue("Description3")
        row.createCell(14).setCellValue("Sedol")
        row.createCell(15).setCellValue("ISIN")
        row.createCell(16).setCellValue("Ticker")
        row.createCell(17).setCellValue("FX rate")
        row.createCell(18).setCellValue("Long/Short")
        row.createCell(19).setCellValue("Currency")
        row.createCell(20).setCellValue("Custodian")
        row.createCell(21).setCellValue("Security Exchange Listing")
        row.createCell(22).setCellValue("FX rate")
      }
      case "Cash appraisal report" => {
        row.createCell(0).setCellValue("LongShortDescription")
        row.createCell(1).setCellValue("GroupByInfo")
        row.createCell(2).setCellValue("InvestDescription")
        row.createCell(3).setCellValue("InvestID")
        row.createCell(4).setCellValue("Quantity")
        row.createCell(5).setCellValue("FXRate")
        row.createCell(6).setCellValue("CostBook")
        row.createCell(7).setCellValue("MarketValueBook")
        row.createCell(8).setCellValue("BookUnrealizedGrainLoss")
        row.createCell(9).setCellValue("percentInvest")
        row.createCell(10).setCellValue("percentsign")
        row.createCell(11).setCellValue("TB name")
        row.createCell(12).setCellValue("PwC Mapping")
      }
      case "Purchase and sale transaction report" => {
        row.createCell(0).setCellValue("TradeDate")
        row.createCell(1).setCellValue("SettleDate")
        row.createCell(2).setCellValue("TranType")
        row.createCell(3).setCellValue("InvestID")
        row.createCell(4).setCellValue("Investment")
        row.createCell(5).setCellValue("CustodianAccount")
        row.createCell(6).setCellValue("Quantity")
        row.createCell(7).setCellValue("Price")
        row.createCell(8).setCellValue("SEC")
        row.createCell(9).setCellValue("LocalAmount")
        row.createCell(10).setCellValue("BookAmount")
        row.createCell(11).setCellValue("ContractDate")
        row.createCell(12).setCellValue("TranID")
        row.createCell(13).setCellValue("GenericInvestment")
        row.createCell(14).setCellValue("Broker")
        row.createCell(15).setCellValue("Trader")
        row.createCell(16).setCellValue("Commission")
        row.createCell(17).setCellValue("Expenses")
        row.createCell(18).setCellValue("LocalCurrency")
        row.createCell(19).setCellValue("TotalBookAmount")
        row.createCell(20).setCellValue("Portfolio")
        row.createCell(21).setCellValue("Fund Currency")
        row.createCell(22).setCellValue("Long/Short")
        row.createCell(23).setCellValue("Sedol")
        row.createCell(24).setCellValue("ISIN")
        row.createCell(25).setCellValue("Ticker")
      }
      case "Dividends Detail Report" => {
        row.createCell(0).setCellValue("Sort")
        row.createCell(1).setCellValue("Currency")
        row.createCell(2).setCellValue("CustAccount")
        row.createCell(3).setCellValue("Investment")
        row.createCell(4).setCellValue("InvID")
        row.createCell(5).setCellValue("TransID")
        row.createCell(6).setCellValue("ExDate")
        row.createCell(7).setCellValue("ExDateQuantity")
        row.createCell(8).setCellValue("LocalDividendPerShareAmount")
        row.createCell(9).setCellValue("WHTaxRate")
        row.createCell(10).setCellValue("LocalGrossDividendIncExp")
        row.createCell(11).setCellValue("LocalNetDividendIncExp")
        row.createCell(12).setCellValue("LocalWHTax")
        row.createCell(13).setCellValue("BookGrossDividendIncExp")
        row.createCell(14).setCellValue("BookNetDividendIncExp")
        row.createCell(15).setCellValue("BookWHTax")
        row.createCell(16).setCellValue("PayDate")
        row.createCell(17).setCellValue("LocalReclaim")
        row.createCell(18).setCellValue("BookReclaim")
        row.createCell(19).setCellValue("Sedol")
        row.createCell(20).setCellValue("ISIN")
        row.createCell(21).setCellValue("Ticker")
        row.createCell(22).setCellValue("ISIN")
        row.createCell(23).setCellValue("Ticker")
        row.createCell(24).setCellValue("Investment")
      }
      case "Realised Gain Loss report" => {
        row.createCell(0).setCellValue("Group1")
        row.createCell(1).setCellValue("InvestmentCode")
        row.createCell(2).setCellValue("CloseDate")
        row.createCell(3).setCellValue("ClosingID")
        row.createCell(4).setCellValue("TransactionType")
        row.createCell(5).setCellValue("TaxLotDate")
        row.createCell(6).setCellValue("TaxLotID")
        row.createCell(7).setCellValue("TaxLotPrice")
        row.createCell(8).setCellValue("ClosingPrice")
        row.createCell(9).setCellValue("OriginalFace")
        row.createCell(10).setCellValue("QuantityOrCurrentFace")
        row.createCell(11).setCellValue("NetProceedsBook")
        row.createCell(12).setCellValue("CostBook")
        row.createCell(13).setCellValue("PriceGLBook")
        row.createCell(14).setCellValue("FXGainLoss")
        row.createCell(15).setCellValue("STCapitalGL")
        row.createCell(16).setCellValue("LTCapitalGL")
        row.createCell(17).setCellValue("OrdinaryIncome")
        row.createCell(18).setCellValue("TotalGLBook")
        row.createCell(19).setCellValue("NetProceedsLocal")
        row.createCell(20).setCellValue("CostLocal")
        row.createCell(21).setCellValue("PriceGLLocal")
        row.createCell(22).setCellValue("Sedol")
        row.createCell(23).setCellValue("ISIN")
        row.createCell(24).setCellValue("Ticker")
        row.createCell(25).setCellValue("Currency")
      }
      case _ => {}
    }

  }

  def outputExcelTableBodyRow(sheet: XSSFSheet, bodyBlocks: Seq[BlockConfig], cellStyle: CellStyle):Unit = {}

  def readCSVFileAndExtractToObject(sourceFilePath: String, extractorConfig: ExtractorConfig, fundEngagementId:String, auditPeriodBegin: Date, auditPeriodEnd: Date):Any = {
    var ofstream: FileOutputStream = null
    this.reportConfig = extractorConfig
    //extract csv file
    val newStorage = extractToObject(sourceFilePath, fundEngagementId, auditPeriodBegin, auditPeriodEnd)
    newStorage
  }

  def extractToObject(sourceFilePath: String, frontEndFundName:String, auditPeriodBegin: Date, auditPeriodEnd: Date) = {
    val content = fileReader.extractTextFromFile(sourceFilePath)
    this.setLineBreaker(dataSanitizer.detectLinebreaker(content))

    var hasReportFound = false
    var newStorage: Any = new GenevaSingleEGAStorage()

    var tableConfig = reportConfig.tableConfig
    var (reportHeaders, tableHeaders, tablebodies): (String, String, Seq[BlockConfig]) = getReportDetails(content, tableConfig)

    if(tablebodies.length == 0 || (tablebodies.length == 1 && tablebodies(0).content == "")) {
      throw ExtractionException("File contains 0 records, please check your file and upload again.")
    }
    var headerStrList = reportHeaders.split("\r\n")
    //verify whether the uploaded excel is valid
    import java.text.SimpleDateFormat
    var formatter = new SimpleDateFormat("MM/dd/yyyy")
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
    var excelPeriodStartStrList = Seq[String]()
    var excelPeriodStartDate = ""
    var excelPeriodEndStrList = Seq[String]()
    var excelPeriodEndDate = ""
    var fileStartDate = LocalDate.now()
    var fileEndDate = LocalDate.now()
    breakable{
      for(i <-0 to headerStrList.length-1){
        if(headerStrList(i).contains("Period Start Date")) {
          if(headerStrList(i).contains(",")) {
            excelPeriodStartStrList = headerStrList(i).split(",")
          } else if(headerStrList(i).contains(":")) {
            excelPeriodStartStrList = headerStrList(i).split(":")
          }
          if(excelPeriodStartStrList.length > 1) {
            excelPeriodStartDate = excelPeriodStartStrList(1).replace("\"", "")
            println("MG: excelPeriodStartDate IS" + excelPeriodStartDate.substring(1, excelPeriodStartDate.length-1))
            fileStartDate = date2UTCLocalDate(formatter.parse(excelPeriodStartDate))
          }
        }
        if(headerStrList(i).contains("Period End Date")){
          if(headerStrList(i).contains(",")) {
            excelPeriodEndStrList = headerStrList(i).split(",")
          } else if(headerStrList(i).contains(":")) {
            excelPeriodEndStrList = headerStrList(i).split(":")
          }
          if(excelPeriodEndStrList.length > 1) {
            excelPeriodEndDate = excelPeriodEndStrList(1).replace("\"", "")
            fileEndDate = date2UTCLocalDate(formatter.parse(excelPeriodEndDate))
          }
          break()
        }
      }
    }

    if(tableHeaders != ""){
      hasReportFound = true
      reportConfigGenerator.initiateTableHeaderStartIndex(tableHeaders, tableConfig.tableBodyConfig,this.lineBreaker)
      newStorage = outputTableBodyRowIntoObjectForPeriod(tableConfig.tableBodyConfig, tablebodies, this.lineBreaker, auditPeriodBegin, auditPeriodEnd, fileStartDate, fileEndDate, true)
    }
    if(!hasReportFound){
      SafeLogger.logString(Logger, s"No report found for fund ${frontEndFundName} in period from ${new Date(auditPeriodBegin.getTime)} to ${new Date(auditPeriodEnd.getTime)}")
    }

    newStorage
  }

}
