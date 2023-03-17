package com.pwc.ds.awm.processor

import java.io.{File, FileOutputStream, IOException}
import java.sql.Date
import java.time.LocalDate
import java.util.TimeZone
import com.pwc.ds.awm.component.safelogger.SafeLogger
import com.pwc.ds.awm.const.ExtractionErrorCode
import com.pwc.ds.awm.processor.OutputMode.Type
import com.pwc.ds.awm.processor.utils.{BaseFileReader, FileReader}
import com.pwc.ds.awm.processor.reportconfigs.extractorschema._
import com.pwc.ds.awm.processor._
import com.pwc.ds.awm.processor.exceptions.{ExtractionError, ExtractionException}
import com.pwc.ds.awm.processor.generateEGA.basicReport.SingleEGAStorage
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import play.api.Logger

import util.control.Breaks._


abstract class BaseReportProcessor() extends AbstractProcessor {


  var lineBreaker:String = ""

  var reportConfig: ExtractorConfig = null
  val outputExcel:BaseExcelOutput = new BaseExcelOutput;
  val reportConfigGenerator: ReportConfigGenerator = new ReportConfigGeneratorImpl {}
  val dataSanitizer:BaseDataSanitizerImpl = new BaseDataSanitizerImpl {}
  val blockSeporator:BlockSeparatorImpl = new BlockSeparatorImpl {}
  val fileReader = new FileReader

  /**
   * child class need to implement this function
   * @param reportHeader
   * @param lineBreaker
   * @return
   */
  def outputTableBodyRowIntoMemory(tableHeaderColumns: Seq[Seq[ColumnConfig]], txtTableBodyPages: Seq[Seq[BlockConfig]], txtReportHeaders:Seq[String], lineBreaker:String) = {

  }

  def outputTableBodyRowIntoObjectForPeriod(tableHeaderColumns: Seq[Seq[ColumnConfig]], txtTableBodyPages: Seq[Seq[BlockConfig]], txtReportHeaders:Seq[String], lineBreaker:String, frontEndPeriodStart: Date, frontEndPeriodEnd: Date):Any = {
    new SingleEGAStorage()
  }
  /**
   * child class need to implement this function
   * @param reportHeader
   * @param lineBreaker
   * @return
   */
  def getFundName(reportHeader:String, lineBreaker:String):String = {
    ""
  }

  /**
   * child class need to implement this function
   * @param reportHeader
   * @param lineBreaker
   * @return
   */
  def getReportYear(reportHeader:String, lineBreaker:String): Int = {
    2018
  }

  def isReportYearValid(txtYear:Integer, frontEndYear:Integer):Boolean ={
    return true
  }

  /**
   * to decide if the fund name is same with the fund name from front end
   * @param txtFundName
   * @param frontEndFundName
   * @return
   */
  def isFundNameValid(txtFundName:String, frontEndFundName:String):Boolean = {
    txtFundName.trim.toUpperCase.contains(frontEndFundName.trim.toUpperCase())
  }


  def setLineBreaker(content: String) = {
    this.lineBreaker = dataSanitizer.detectLinebreaker(content)
  }

  /**
   * 1. remove the empty lines in the start or end of in the block content.
   * 2. also replace dailytotal,grandtotal these words with whitespaces in case it affects the column span setting afterwards
   *
   * @param block
   * @return
   */
  def organizeBlock(block: BlockConfig): BlockConfig ={

    block.content = block.content.replaceAll("RESTRICTED\\.*","")
    if(!dataSanitizer.isTableContent(block.content)){
      block.blockType = BlockType.discard
    }
    else if(block.content.contains(BlockType.grandTotal.toString)){
      block.content = block.content.replaceAll(BlockType.grandTotal.toString, "           ")
      block.blockType = BlockType.grandTotal
    }
    var content = dataSanitizer.trimEmptyStartLines(block.content, lineBreaker)
    content = dataSanitizer.trimEmptyEndLines(content, lineBreaker)
    block.content = content
    block
  }


  def extractToExcel(sourceFilePath: String): XSSFWorkbook = {

    val workbook = new XSSFWorkbook()

    val content = fileReader.extractTextFromFile(sourceFilePath)

    this.setLineBreaker(dataSanitizer.detectLinebreaker(content))

    var sheets:Array[String] = blockSeporator.splitContentIntoSheets(content);

    for(sheetNum <- 0 until sheets.length){

      var sheetContent = sheets(sheetNum);

      var pages:Array[String] = blockSeporator.splitContentIntoPages(sheetContent, this.lineBreaker) // the pages must contain word "PAGE"

      var bodySheet = workbook.createSheet("result_" + (sheetNum + 1))

      var hasSummary = reportConfig.summaryConfig.isDefined
      var summaries = getPageBlocksAndInputToExcel(bodySheet, pages, reportConfig.tableConfig, pages.length, hasSummary)


      if(reportConfig.summaryConfig.isDefined && summaries.filter(_.trim.length>0).length > 0){

        var summaryConfig = reportConfig.summaryConfig.get
        var summarySheet = workbook.createSheet("summary_for_report_" + (sheetNum + 1))
        getPageBlocksAndInputToExcel(summarySheet, summaries.toArray, summaryConfig, pages.length)
      }

    }
    workbook
  }

  def extractToMemory(sourceFilePath: String, frontEndFundName:String, frontEndYear:Integer) = {
    val content = fileReader.extractTextFromFile(sourceFilePath)
    this.setLineBreaker(dataSanitizer.detectLinebreaker(content))
    var sheets:Array[String] = blockSeporator.splitContentIntoSheets(content).filter(content => content.contains(frontEndFundName))
    var hasReportFound = false
    breakable{
      for(sheet <- sheets){
        var pages:Array[String] = blockSeporator.splitContentIntoPages(sheet, this.lineBreaker) // the pages must contain word "PAGE"
        var hasSummary = reportConfig.summaryConfig.isDefined
        var (txtReportHeaders,txtTableHeaders, txtTablebodies, sumaries):(Array[String],Array[String],Seq[Seq[BlockConfig]], Seq[String]) = getPageDetailedBlocks(pages, reportConfig.tableConfig, hasSummary);

        var txtReportHeadersWithContent = txtReportHeaders.filter(header => {header.trim.length > 0})
        var txtTableHeadersWithContent = txtTableHeaders.filter(header => {header.trim.length > 0})
        if(txtReportHeadersWithContent.length > 0){
          var txtYear = this.getReportYear(txtReportHeadersWithContent(0), this.lineBreaker)
          var txtFundName = this.getFundName(txtReportHeadersWithContent(0), this.lineBreaker)
          if(isReportYearValid(txtYear, frontEndYear) && isFundNameValid(txtFundName, frontEndFundName)){
            hasReportFound = true
            var tableHeaderColumns = reportConfig.tableConfig.tableBodyConfig
            reportConfigGenerator.initiateTableHeaderStartIndex(txtTableHeadersWithContent(0), tableHeaderColumns,this.lineBreaker)
            reportConfigGenerator.setColumnStartAccordingToTableContent(txtTablebodies, tableHeaderColumns, this.lineBreaker)
            reportConfigGenerator.setColumnEndIndex(tableHeaderColumns)
            outputTableBodyRowIntoMemory(tableHeaderColumns, txtTablebodies, txtReportHeaders, this.lineBreaker)
            break
          }
        }

      }
    }
    if(!hasReportFound){
      SafeLogger.logString(Logger, s"No report found for fund ${frontEndFundName} in year ${frontEndYear}")
    }
  }


  def getPageBlocksAndInputToExcel(sheet:XSSFSheet, pages:Array[String],
                                   tableConfig: TableConfig, totalPageNum :Int= 0, hasSummary:Boolean = false):Seq[String] = {

    var tableHeaderColumns = tableConfig.tableBodyConfig

    var (txtReportHeaders,txtTableHeaders, txtTablebodies, sumaries):(Array[String],Array[String],Seq[Seq[BlockConfig]], Seq[String]) = getPageDetailedBlocks(pages, tableConfig, hasSummary);

    var txtHeadersWithContent = txtTableHeaders.filter(header => {
      header.trim.length > 0
    })

    if(txtTablebodies.flatten.length >0 && txtHeadersWithContent.length > 0 && txtTableHeaders.length > 0){ //only there are contents, we output to the excel
      reportConfigGenerator.initiateTableHeaderStartIndex(txtHeadersWithContent(0), tableHeaderColumns,this.lineBreaker)

      reportConfigGenerator.setColumnStartAccordingToTableContent(txtTablebodies, tableHeaderColumns, this.lineBreaker)

      reportConfigGenerator.setColumnEndIndex(tableHeaderColumns)

      outputExcel.outputExcelTableHeaderRow(sheet, tableHeaderColumns, Some(0));
      outputExcel.outputExcelTableBodyRow(sheet, tableHeaderColumns, txtTablebodies,txtReportHeaders,this.lineBreaker, totalPageNum)
    }
    sumaries
  }

  /**
   * @param pages: separate pages into reportHeaders, tableHeaders, tableBodies, summaries. (summary must be embraced with star(*) lines.
   * @param blockType: define how many line breaks exist in table body.
   * @param reportHeaderLineNum
   * @param tableHeaderLineNum
   * @return  reportHeaders, tableHeaders, summaries are aligned with page number
   */
  def getPageDetailedBlocks(pages:Array[String], tableConfig:TableConfig, hasSummary:Boolean): (Array[String],Array[String],Seq[Seq[BlockConfig]], Seq[String])={
    var reportHeaders = new Array[String](pages.length)
    var tableHeaders = new Array[String](pages.length)
    var tablebodies = Seq[Seq[BlockConfig]]()
    var summaries = Seq[String]()
    for(pageNum <- 0 until pages.length){

      var headerStr = ""
      var txtTableHeaderStr = ""
      var pageBlocks = Seq[BlockConfig]()
      if(!dataSanitizer.isGarbageContent(pages(pageNum))){ // when page is summary page, it's empty content
        var page = pages(pageNum)
        var left = dataSanitizer.trimEmptyStartLines(page, this.lineBreaker)
        var reportHeaderLastIndex = blockSeporator.getReportHeaderEndIndex(left,tableConfig.reportHeaderLineNumber, lineBreaker);
        headerStr = left.substring(0, reportHeaderLastIndex);
        left = left.substring(reportHeaderLastIndex)

        var (summaryContent:String, tableBodyContent:String) = if(hasSummary) blockSeporator.getSummaryContent(left,lineBreaker) else ("",left)// here get summary content by star (*) lines
        summaries = summaries :+ summaryContent
        left = dataSanitizer.trimEmptyStartLines(tableBodyContent, this.lineBreaker);

        var tableHeaderLastIndex = blockSeporator.getTableHeaderEndIndex(left,tableConfig.tableHeaderLineNumber, lineBreaker);
        txtTableHeaderStr = if(tableHeaderLastIndex>0)left.substring(0, tableHeaderLastIndex) else "";
        left = if(tableHeaderLastIndex>0)left.substring(tableHeaderLastIndex) else ""

        pageBlocks = blockSeporator.splitTableContentIntoBlocks(tableConfig.blockSeperatorNumber, tableConfig.tableHeaderLineNumber, left, lineBreaker);

        pageBlocks = pageBlocks.map(block => {
          block.pageNum = pageNum + 1;
          organizeBlock(block)
        })

        pageBlocks = pageBlocks.filter(block => {
          block.blockType != BlockType.discard
        })
      }
      reportHeaders(pageNum) = headerStr;
      tableHeaders(pageNum) = txtTableHeaderStr;
      tablebodies = tablebodies :+ pageBlocks
    }
    return (reportHeaders, tableHeaders, tablebodies, summaries)
  }


  def readFileAndExtract(sourceFilePath: String, destFilePath: String, mode: Type, extractorConfig: ExtractorConfig, fundName:String, year: Integer): Unit = {
    var ofstream: FileOutputStream = null
    this.reportConfig = extractorConfig
    try {
      mode match {
        case OutputMode.Excel =>{
          val workbook = extractToExcel(sourceFilePath)
          val outfile = new File(destFilePath)
          ofstream = new FileOutputStream(outfile)
          if (!outfile.exists()) outfile.createNewFile()
          workbook.write(ofstream)
          ofstream.flush()
          ofstream.close()
        }
        case OutputMode.Memory =>{
          extractToMemory(sourceFilePath, fundName, year)
        }
      }

    } catch {
      case e: Exception => SafeLogger.logStringError(Logger, s"Error when extracting ${extractorConfig.reportType} for fund(${fundName}) year ${year}", e)
    } finally {
      try {
        if (ofstream != null) ofstream.close()
      } catch {
        case e: IOException => SafeLogger.logStringError(Logger, s"Error when saving file for ${extractorConfig.reportType} for fund(${fundName}) year ${year}", e)
      }
    }
  }

  def isReportDateValid( periodStart: Date, periodEnd: Date, date1: Date, date2: Date = null): Boolean = {
    !date2UTCLocalDate(date1).isAfter(date2UTCLocalDate(periodEnd)) && !date2UTCLocalDate(date1).isBefore(date2UTCLocalDate(periodStart))
  }

  def isCurrentYear(periodStart: Date, periodEnd: Date, date1: Date, date2:Date = null): Boolean = {
    !date2UTCLocalDate(date2).isAfter(date2UTCLocalDate(periodEnd)) && !date2UTCLocalDate(date1).isBefore(date2UTCLocalDate(periodStart))
  }

  def isPreviousYear(periodStart: Date, periodEnd: Date, date1: Date, date2:Date = null): Boolean = {
    date2UTCLocalDate(date1).isBefore(date2UTCLocalDate(periodStart))
  }
  def isAfterYear(periodStart: Date, periodEnd: Date, date1: Date, date2:Date = null): Boolean = {
    date2UTCLocalDate(date1).isAfter(date2UTCLocalDate(periodEnd))
  }

  def getReportStartDate(reportHeader: String, lineBreaker: String): Date = {
    null
  }
  def getReportEndDate(reportHeader: String, lineBreaker: String): Date = {
    null
  }


  /**
   *
   * Below is the methods to extract to object, this is for HSBC IMS extraction
   */

  def extractToObjectForPeriod(sourceFilePath: String, frontEndFundName: String, frontEndPeriodStart: Date, frontEndPeriodEnd: Date): Any = {
    val content = fileReader.extractTextFromFile(sourceFilePath)
    this.setLineBreaker(dataSanitizer.detectLinebreaker(content))
    var sheets:Array[String] = blockSeporator.splitContentIntoSheets(content).filter(content => content.contains(frontEndFundName))

    var hasReportFound = false
    var newStorage:Any = null
    breakable{
      for(sheet <- sheets){
        var pages:Array[String] = blockSeporator.splitContentIntoPages(sheet, this.lineBreaker) // the pages must contain word "PAGE"
        var hasSummary = reportConfig.summaryConfig.isDefined
        var (txtReportHeaders,txtTableHeaders, txtTablebodies, sumaries):(Array[String],Array[String],Seq[Seq[BlockConfig]], Seq[String]) = getPageDetailedBlocks(pages, reportConfig.tableConfig, hasSummary);

        var txtReportHeadersWithContent = txtReportHeaders.filter(header => {header.trim.length > 0})
        var txtTableHeadersWithContent = txtTableHeaders.filter(header => {header.trim.length > 0})
        if(txtReportHeadersWithContent.length > 0){
          var date1 = this.getReportStartDate(txtReportHeadersWithContent(0), this.lineBreaker)
          var date2 =  this.getReportEndDate(txtReportHeadersWithContent(0), this.lineBreaker)
          var txtFundName = this.getFundName(txtReportHeadersWithContent(0), this.lineBreaker)
          if (isFundNameValid(txtFundName, frontEndFundName) && isReportDateValid(frontEndPeriodStart, frontEndPeriodEnd, date1, date2)) {
            hasReportFound = true
            var tableHeaderColumns = reportConfig.tableConfig.tableBodyConfig
            reportConfigGenerator.initiateTableHeaderStartIndex(txtTableHeadersWithContent(0), tableHeaderColumns,this.lineBreaker)
            reportConfigGenerator.setColumnStartAccordingToTableContent(txtTablebodies, tableHeaderColumns, this.lineBreaker)
            reportConfigGenerator.setColumnEndIndex(tableHeaderColumns)
            newStorage = outputTableBodyRowIntoObjectForPeriod(tableHeaderColumns, txtTablebodies, txtReportHeaders, this.lineBreaker, frontEndPeriodStart, frontEndPeriodEnd)
            break
          }
        }
      }
    }

    if(!hasReportFound){
      SafeLogger.logString(Logger, s"No report found for fund ${frontEndFundName} in period ${frontEndPeriodStart} ${frontEndPeriodEnd}")
      throw ExtractionError(ExtractionErrorCode.ZERO_RECORDS_EXTRACTED)
      //throw ExtractionException(s"No report found for fund ${frontEndFundName}.")
    }

    newStorage
  }

  override def readFileAndExtractToObjectForPeriod(sourceFilePath: String, extractorConfig: ExtractorConfig, fundName: String, frontEndPeriodStart: Date, frontEndPeriodEnd: Date): Any = {
    var ofstream: FileOutputStream = null
    this.reportConfig = extractorConfig
    val newStorage = extractToObjectForPeriod(sourceFilePath, fundName, frontEndPeriodStart, frontEndPeriodEnd)
    newStorage
  }

  /**
   * @param content     : separate csv file content into reportHeaders, tableHeaders, tableBodies.
   * @param tableConfig
   * @return reportHeaders, tableHeaders, tableBodies
   */
  def getReportDetails(content: String, tableConfig: TableConfig): (String, String, Seq[BlockConfig]) = {
    var reportHeaders = ""
    var tableHeaders = ""
    var tablebodies = Seq[BlockConfig]()

    var tableConfig = reportConfig.tableConfig
    //trim empty start lines
    var left = dataSanitizer.trimEmptyStartLines(content, lineBreaker);
    //get the report header end index
    var reportHeaderLastIndex = blockSeporator.getReportHeaderEndIndex(left, tableConfig.reportHeaderLineNumber, lineBreaker);
    //get the report header string which contains multiple lines
    reportHeaders = left.substring(0, reportHeaderLastIndex);

    //get the string without the report headers
    left = left.substring(reportHeaderLastIndex)

    left = dataSanitizer.trimEmptyStartLines(left, this.lineBreaker);
    var tableHeaderLastIndex = blockSeporator.getTableHeaderEndIndex(left, tableConfig.tableHeaderLineNumber, this.lineBreaker);
    tableHeaders = if (tableHeaderLastIndex > 0) left.substring(0, tableHeaderLastIndex) else "";

    //get the string without the report headers and table headers
    left = if (tableHeaderLastIndex > 0) left.substring(tableHeaderLastIndex) else ""

    if (tableHeaders.length > 0) {
      left = dataSanitizer.trimEmptyStartLines(left, lineBreaker)
      tablebodies = blockSeporator.splitTableContentIntoBlocks(tableConfig.blockSeperatorNumber, tableConfig.tableHeaderLineNumber, left, this.lineBreaker)
      tablebodies = tablebodies.map(block => {
        organizeBlock(block)
      })
    }
    return (reportHeaders, tableHeaders, tablebodies)
  }


}
