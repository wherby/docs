package com.pwc.ds.awm.processor.assetadmin.myob.reader

import com.pwc.ds.awm.component.excel.{ExcelField, ExcelFieldType, ExcelToArray}
import com.pwc.ds.awm.processor.{
  NA_STR,
  convertDateStringToYMDString,
  changeDateFromDDMMYYYToMMDDYYYY
}
import com.pwc.ds.awm.processor.generalExcel.ExcelReaderHelper.{
  getValueOfExcelField,
  stringNormalizer
}
import com.pwc.ds.awm.processor.generalExcel.{
  BasicExcelReader,
  BasicExcelReaderHelper,
  ExcelReaderHelper
}
import com.pwc.ds.awm.processor.singleegarow.GLRow.{
  Balance,
  BookAmount,
  EndingBalanceAmount,
  TranDate,
  TranDescription,
  TranID,
  createGLRowMap
}

object MyobGLReader {
  val headerStr1 = "ID#\tSrc\tDate\tMemo\tDebit\tCredit\tNet Activity\tEnding Balance"
  val headerStr2 = "Src\tDate\tMemo\tDebit\tCredit\tNet Activity\tEnding Balance"
  val headerStr3 = "ID#\tSrc\tDate\tMemo\tDebit\tCredit\tJob No.\tNet Activity\tEnding Balance"
  val headerStr4 = "編號#\t類別\t日期\t備忘\t借項\t貸項\t項目編號\t淨活動\t最後結餘"

  val lenthFilter              = BasicExcelReaderHelper.basicRowLengthFilter(8)
  val headerTitleFilter1       = BasicExcelReaderHelper.basicHeaderTitleFilter(2, "Src")
  val emptyFilter1             = BasicExcelReaderHelper.basicEmptyFilter(1)
  val emptyFilter12            = BasicExcelReaderHelper.basicEmptyFilter(3)
  val emptyFilter13            = BasicExcelReaderHelper.basicEmptyFilter(8)
  val headerTitleFilterChinese = BasicExcelReaderHelper.basicHeaderTitleFilter(2, "類別")

  def glExcelReader(header: String): String => Seq[Seq[ExcelField]] =
    (filePath: String) => {
      val allSheet          = ExcelToArray.multiSheetToArray(filePath)
      val headerNormalized  = ExcelReaderHelper.stringNormalizer(header)
      val res               = ExcelReaderHelper.getSpecifiedSheetWithoutReplacement(allSheet, headerNormalized)
      val res2              = ExcelReaderHelper.leftEmptyFilter(res)
      val pagedField        = ExcelReaderHelper.getPaged(res2, pageEndChecker)
      val updatedContent    = handlePagedContent(pagedField)
      val dateFormatContent = changeDateFromDDMMYYYToMMDDYYYY(updatedContent, 3)
      dateFormatContent
    }

  def addEmptyToLeft(lines: Seq[Seq[ExcelField]]): Seq[Seq[ExcelField]] = {
    lines.map { line =>
      Seq(ExcelField(ExcelFieldType.StringType, NA_STR)) ++ line
    }
  }

  def glExcelReader2(header: String): String => Seq[Seq[ExcelField]] =
    (filePath: String) => {
      val allSheet         = ExcelToArray.multiSheetToArray(filePath)
      val headerNormalized = ExcelReaderHelper.stringNormalizer(header)
      val res = ExcelReaderHelper.getSpecifiedSheetWithoutReplacementWithLeftAligned(
        allSheet,
        headerNormalized
      )
      val res2              = addEmptyToLeft(res)
      val pagedField        = ExcelReaderHelper.getPaged(res2, pageEndChecker)
      val updatedContent    = handlePagedContent(pagedField)
      val dateFormatContent = changeDateFromDDMMYYYToMMDDYYYY(updatedContent, 3)
      dateFormatContent
    }

  def glExcelReader3(header: String): String => Seq[Seq[ExcelField]] =
    (filePath: String) => {
      val allSheet          = ExcelToArray.multiSheetToArray(filePath)
      val headerNormalized  = ExcelReaderHelper.stringNormalizer(header)
      val res               = ExcelReaderHelper.getSpecifiedSheetWithoutReplacement(allSheet, headerNormalized)
      val res11             = ExcelReaderHelper.leftEmptyFilter(res)
      val res2              = removeJobNoColumn(res11)
      val pagedField        = ExcelReaderHelper.getPaged(res2, pageEndChecker)
      val updatedContent    = handlePagedContent(pagedField)
      val dateFormatContent = changeDateFromDDMMYYYToMMDDYYYY(updatedContent, 3)
      dateFormatContent
    }

  def glExcelReader4(header: String): String => Seq[Seq[ExcelField]] =
    (filePath: String) => {
      val allSheet          = ExcelToArray.multiSheetToArray(filePath)
      val headerNormalized  = ExcelReaderHelper.stringNormalizer(header)
      val res               = ExcelReaderHelper.getSpecifiedSheetWithoutReplacement(allSheet, headerNormalized)
      val res11             = ExcelReaderHelper.leftEmptyFilter(res)
      val res2              = removeJobNoColumn(res11)
      val pagedField        = ExcelReaderHelper.getPaged(res2, pageEndCheckerChinese)
      val updatedContent    = handlePagedContent(pagedField)
      val dateFormatContent = changeDateFromDDMMYYYToMMDDYYYY(updatedContent, 3)
      dateFormatContent
    }
  private def removeJobNoColumn(lines: Seq[Seq[ExcelField]]): Seq[Seq[ExcelField]] = {
    lines.map { line =>
      if (line.length > 9) {
        line.take(6) ++ line.drop(7)
      } else {
        line
      }
    }
  }

  def pageEndChecker(line: Seq[ExcelField]): Boolean = {
    line.length > 4 && line(3).field == "Total:"
  }

  def pageEndCheckerChinese(line: Seq[ExcelField]): Boolean = {
    line.length > 4 && line(3).field == "總計:"
  }

  def handlePagedContent(pagedContents: Seq[Seq[Seq[ExcelField]]]): Seq[Seq[ExcelField]] = {
    pagedContents.map { pageContent =>
      if (pagedContents.length > 0 && pageContent(pageContent.length - 1).length > 7) {
        val endBalanceValue  = pageContent(pageContent.length - 1)(7).field
        val endBalanceValue2 = ExcelReaderHelper.getNumberStrValue(endBalanceValue)
        pageContent.map { line =>
          val endBalanceExcelField = ExcelField(ExcelFieldType.StringType, endBalanceValue2)
          endBalanceExcelField +: line
        }
      } else {
        pageContent
      }
    }.flatten
  }

  def createRow(row: Seq[ExcelField]) = {
    (createGLRowMap() ++ Map(
      TranDate            -> convertDateStringToYMDString(getValueOfExcelField(row, 3)),
      TranID              -> getValueOfExcelField(row, 1),
      TranDescription     -> getValueOfExcelField(row, 4),
      BookAmount          -> getValueOfExcelField(row, 5),
      Balance             -> getValueOfExcelField(row, 6),
      EndingBalanceAmount -> getValueOfExcelField(row, 0)
    )).toMap
  }

  val setting1 = BasicExcelReader(
    headerStr1,
    Seq(lenthFilter, headerTitleFilter1, emptyFilter1, emptyFilter12, emptyFilter13),
    createRow,
    Some(glExcelReader(headerStr1))
  )

  val setting2 = BasicExcelReader(
    headerStr2,
    Seq(lenthFilter, headerTitleFilter1, emptyFilter1, emptyFilter12, emptyFilter13),
    createRow,
    Some(glExcelReader2(headerStr2))
  )

  val setting3 = BasicExcelReader(
    headerStr3,
    Seq(lenthFilter, headerTitleFilter1, emptyFilter1, emptyFilter12, emptyFilter13),
    createRow,
    Some(glExcelReader3(headerStr3))
  )

  val setting4 = BasicExcelReader(
    headerStr4,
    Seq(lenthFilter, headerTitleFilterChinese, emptyFilter1, emptyFilter12, emptyFilter13),
    createRow,
    Some(glExcelReader4(headerStr4))
  )
}