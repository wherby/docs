package com.pwc.ds.awm.processor.excelReport

import java.io.FileOutputStream
import java.sql.Date
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.TimeZone

import com.pwc.ds.awm.component.safelogger.SafeLogger
import com.pwc.ds.awm.const.ExtractionErrorCode
import com.pwc.ds.awm.processor.date2UTCLocalDate
import com.pwc.ds.awm.processor.exceptions.{ExtractionError, ExtractionException}
import com.pwc.ds.awm.processor.generateEGA.basicReport.GenevaSingleEGAStorage
import com.pwc.ds.awm.processor.reportconfigs.extractorschema.{BlockConfig, ColumnConfig, ExtractorConfig, TableConfig}
import com.pwc.ds.awm.processor.singleegarow.PositionRow
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.xssf.usermodel.XSSFSheet
import play.api.Logger

import scala.collection.Map
import scala.util.control.Breaks.{break, breakable}

class PositionAppraisalReportProcessor extends BaseExcelReportProcessor {

  override  def outputTableBodyRowIntoObjectForPeriod(tableHeaderColumns: Seq[Seq[ColumnConfig]], bodyBlocks: Seq[BlockConfig], lineBreaker: String, frontEndPeriodStart: Date, frontEndPeriodEnd: Date, fileStartDate: LocalDate, fileEndDate: LocalDate, isCSV: Boolean): Any = {
    var newStorage = new GenevaSingleEGAStorage()

    var regex = "\\|"
    if(isCSV) {
      regex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"
    }

    if(fileEndDate.isBefore(date2UTCLocalDate(frontEndPeriodStart))){
      throw ExtractionException("Report date found to be outside the audit period!")
    } else if (!date2UTCLocalDate(frontEndPeriodStart).isAfter(fileEndDate) && !fileEndDate.isAfter(date2UTCLocalDate(frontEndPeriodEnd))){
      var positionRows: Seq[Map[String, String]] = Seq()

      for (block <- bodyBlocks) {
        var positionRow: Map[String, String] = Map()

        var blocklines = block.content.split(regex, -1);
        var headerLine = tableHeaderColumns(0);
        positionRow = fillPositionRow(blocklines, headerLine)

        if (positionRow != null && !positionRow.isEmpty)
          positionRows = positionRows :+ positionRow
      }
      newStorage.positionAppraisalReportPositionData = positionRows
    }else if (fileEndDate.isAfter(date2UTCLocalDate(frontEndPeriodEnd))) {
      throw ExtractionError(ExtractionErrorCode.AUDIT_PERIOD_MISMATCH)
    }
    newStorage
  }

  def fillPositionRow(bodyLineList: Seq[String], headerLine: Seq[ColumnConfig]): Map[String, String] = {
    var positionRow = PositionRow.createPositionRowMap()
    for (headerColumnNum <- 0 until headerLine.length) {
      var cellValue = bodyLineList(headerColumnNum)
      if(cellValue.startsWith("\"")) {
        cellValue = cellValue.replace("\"", "")
      }
      headerLine(headerColumnNum).title match {
        case "Portfolio" => {positionRow.put(PositionRow.Group1, cellValue); positionRow.put(PositionRow.Portfolio, cellValue)}
        case "Investment Type"=>positionRow.put(PositionRow.Group2, cellValue)
        case "Security ID" => positionRow.put(PositionRow.InvestID, cellValue)
        case "Investment" => positionRow.put(PositionRow.Description, cellValue)
        case "Quantity" => positionRow.put(PositionRow.Quantity, cellValue)
        case "Local Market Price" => positionRow.put(PositionRow.MarketPrice, cellValue)
        case "Current Book Cost" => positionRow.put(PositionRow.CostBook, cellValue)
        case "Local Market Value" => positionRow.put(PositionRow.MarketValueLocal, cellValue)
        case "Book Market Value" => positionRow.put(PositionRow.MarketValueBook, cellValue)
        case "Book Unrealized Gain/Loss" => positionRow.put(PositionRow.UnrealizedGainOrLoss, cellValue)
        case "% NAV" => positionRow.put(PositionRow.PercentAssets, cellValue)
        case "Sedol" => positionRow.put(PositionRow.Sedol, cellValue)
        case "Isin" => positionRow.put(PositionRow.ISIN, cellValue)
        case "Ticker" => positionRow.put(PositionRow.Ticker, cellValue)
        case "Exchange Rate" => {positionRow.put(PositionRow.FXRate, cellValue); positionRow.put(PositionRow.FXRate2, cellValue)}
        case "Long/Short" => positionRow.put(PositionRow.LongShort, cellValue)
        case "Currency" => positionRow.put(PositionRow.Currency, cellValue)
        case "Custodian" => positionRow.put(PositionRow.Custodian, cellValue)
        case "Security Exchange Listing" => positionRow.put(PositionRow.SecurityExchangeListing, cellValue)
        case _ => {}
      }
    }
    positionRow
  }

  override def outputExcelTableBodyRow(sheet: XSSFSheet, bodyBlocks: Seq[BlockConfig], cellStyle: CellStyle): Unit = {
    var rowNum = 1
    for(block <- bodyBlocks){
      var row = sheet.createRow(rowNum);
      var blockLine = block.content;
      var bodyLineList = blockLine.split("\\|", -1)
      row.createCell(0).setCellValue(bodyLineList(0))
      row.createCell(1).setCellValue(bodyLineList(28))
      row.createCell(2).setCellValue(bodyLineList(9))
      row.createCell(3).setCellValue(bodyLineList(0))
      row.createCell(4).setCellValue(bodyLineList(8))
      val cell = row.createCell(5)
      if(bodyLineList(16) != ""){
        cell.setCellValue(bodyLineList(16).toDouble)
      }else {
        cell.setCellValue(bodyLineList(16))
      }
      val cell2 = row.createCell(6)
      if(bodyLineList(17) != ""){
        cell2.setCellValue(bodyLineList(17).toDouble)
      }else{
        cell2.setCellValue(bodyLineList(17))
      }
      val cell3 = row.createCell(7)
      if(bodyLineList(21) != ""){
        cell3.setCellValue(bodyLineList(21).toDouble)
      }else{
        cell3.setCellValue(bodyLineList(21))
      }
      val cell4 = row.createCell(8)
      if(bodyLineList(20) != ""){
        cell4.setCellValue(bodyLineList(20).toDouble)
      }else{
        cell4.setCellValue(bodyLineList(20))
      }
      val cell5 = row.createCell(9)
      if(bodyLineList(22) != ""){
        cell5.setCellValue(bodyLineList(22).toDouble)
      }else{
        cell5.setCellValue(bodyLineList(22))
      }
      val cell6 = row.createCell(10)
      if(bodyLineList(23) != ""){
        cell6.setCellValue(bodyLineList(23).toDouble)
      }else{
        cell6.setCellValue(bodyLineList(23))
      }
      val cell7 = row.createCell(11)
      if(bodyLineList(25) != ""){
        cell7.setCellValue(bodyLineList(25).toDouble)
      }else{
        cell7.setCellValue(bodyLineList(25))
      }
      row.createCell(12).setCellValue("N/A")
      row.createCell(13).setCellValue("N/A")
      row.createCell(14).setCellValue(bodyLineList(11))
      row.createCell(15).setCellValue(bodyLineList(12))
      row.createCell(16).setCellValue(bodyLineList(14))
      val cell8 = row.createCell(17)
      if(bodyLineList(34) != ""){
        cell7.setCellValue(bodyLineList(34).toDouble)
      }else{
        cell7.setCellValue(bodyLineList(34))
      }
      row.createCell(18).setCellValue(bodyLineList(4))
      row.createCell(19).setCellValue(bodyLineList(7))
      row.createCell(20).setCellValue(bodyLineList(5))
      row.createCell(21).setCellValue(bodyLineList(27))
      val cell9 = row.createCell(22)
      if(bodyLineList(34) != ""){
        cell7.setCellValue(bodyLineList(34).toDouble)
      }else{
        cell7.setCellValue(bodyLineList(34))
      }

      rowNum = rowNum + 1;
    }
  }
}
