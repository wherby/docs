package aifss.dataextraction.service

import aifss.dataextraction.models.formulacheck
import aifss.dataextraction.models.formulacheck.{ItemAddList, ItemList, SheetColExt, SheetItemList, StandardMappedToExcel}
import aifss.dataextraction.models.lineitemmap.SheetRowKeyInfo
import aifss.dataextraction.models.testModels.rowKeysList
import aifss.dictionary.service.NewDictionaryService
import aifss.dataextraction.models.formulacheck.testModels.workBookInfoList

import aifss.dataextraction.service.formulacheck.{FormulaCorrector, NotMatchedFormula}
import aifss.dataextraction.service.formulacheck
import org.apache.poi.xssf.usermodel.{XSSFFormulaEvaluator, XSSFWorkbook}

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

class DownloadService @Inject()(dictionaryService: NewDictionaryService)(implicit ec: ExecutionContext) {

  def evaluateExtractedDataToExcel(standardMappedToExcelList: Seq[StandardMappedToExcel],workbook: XSSFWorkbook):Seq[NotMatchedFormula]={
    val notMatchedFormulaList=new ListBuffer[NotMatchedFormula]
    val sheetItemList = new ListBuffer[SheetItemList]
    standardMappedToExcelList.map(standardMappedToExcel=>{
      val standardItemList = new ListBuffer[ItemList]
      val itemAddedList = new ListBuffer[ItemAddList]
      val sheetType = standardMappedToExcel.sheetType
      standardMappedToExcel.standardMappedToExcel.foreach(itemListForSingleItem=>{
        val itemType = itemListForSingleItem.itemType
        if (itemType != "unmap"){
          val sheetColExtList = itemListForSingleItem.sheetColExtList
          val (standardItemForSingleItem, itemAddedForSingleItem)=SubItemHandler.handleSheetSubItem(sheetColExtList)
          standardItemList+=ItemList(itemType,standardItemForSingleItem)
          itemAddedList+=ItemAddList(itemType,itemAddedForSingleItem)
        }
      })
      val unmapSheetItems = standardMappedToExcel.standardMappedToExcel.find(item=>{item.itemType == "unmap"}).map(_.sheetColExtList).getOrElse(new ListBuffer[SheetColExt])
//      if (unmapSheetItems.size == 0) {
//        sheetItemList += SheetItemList(sheetType, standardItemList, itemAddedList, new ListBuffer[SheetColExt],new ListBuffer[SheetColExt])
//      } else {
//        sheetItemList += SheetItemList(sheetType, standardItemList, itemAddedList, new ListBuffer[SheetColExt],unmapSheetItems)
//      }
      if (unmapSheetItems.size == 0) {
        println("0")
      }else {
        println("1")
      }
      println("accc")
    })
    println(sheetItemList.toString())
    workBookInfoList.foreach(workBookInfoForSingleSheet=>{
      val sheetType = workBookInfoForSingleSheet.sheetType
      val workbookName = workBookInfoForSingleSheet.workBookName
      val itemNameList = workBookInfoForSingleSheet.itemNameList
      val rowNameList = workBookInfoForSingleSheet.rowNameList
      val notMatchSheetName = workBookInfoForSingleSheet.notMatchSheetName
      val standardItemList = sheetItemList.find(item=>{item.sheetType == sheetType}).map(_.standardItemList).getOrElse(new ListBuffer[ItemList])
      val dataList = standardItemList.filter(item=>itemNameList.contains(item.itemType))
      val sheetRowKeys = rowKeysList.find(_.sheetType == sheetType).map(_.sheetRowKeys).getOrElse(new ListBuffer[SheetRowKeyInfo])
      val formulaFromSheet = FinancialDataToExcel.writeSheetToExcel(workbookName, workbook, dataList, rowNameList,sheetRowKeys)
      val sheetNotMatch =sheetItemList.find(item=>{item.sheetType == sheetType}).map(_.unmapSheetItems).getOrElse(new ListBuffer[SheetColExt])
      FinancialDataToExcel.writeNotMatchSheetToExcelNotMatch(workbook,sheetNotMatch,notMatchSheetName)
      val evaluator = new XSSFFormulaEvaluator(workbook)
      evaluator.evaluateAll()
      val notMatchedSheet = FinancialDataToExcel.verifySheetToExcel(workbookName,workbook,dataList,rowNameList,sheetRowKeys)
      val originNotMatchedItem = notMatchedSheet.map(x=>x.head)
      val sheetAutoCorrector = FormulaCorrector.tryToCorrectSheet(standardItemList.flatMap(_.sheetColExtList),originNotMatchedItem,sheetType)
      notMatchedFormulaList += NotMatchedFormula(sheetType,notMatchedSheet,sheetAutoCorrector._1,sheetAutoCorrector._2)
    })

    notMatchedFormulaList
  }
}

