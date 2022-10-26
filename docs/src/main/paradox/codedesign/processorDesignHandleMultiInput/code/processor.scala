package com.pwc.ds.awm.processor.assetadmin.myob

import com.pwc.ds.awm.processor.BaseReportProcessor
import com.pwc.ds.awm.processor.assetadmin.myob.reader.MyobGLReader
import com.pwc.ds.awm.processor.generalExcel.{BasicExcelReaderHelper, BasicGLProcessor}
import com.pwc.ds.awm.processor.generateEGA.basicReport.{GeneralSingleEGAStorage, MyobEGAStorage}
import com.pwc.ds.awm.processor.singleegarow.GLRow.TranDate

import java.sql.Date

class MyobGLProcessor extends BaseReportProcessor {
  override def extractToObjectForPeriod(
                                         sourceFilePath: String,
                                         frontEndFundName: String,
                                         frontEndPeriodStart: Date,
                                         frontEndPeriodEnd: Date
                                       ): GeneralSingleEGAStorage = {
    val storage = new MyobEGAStorage()
    val tableRow1 = BasicExcelReaderHelper.processFileOnExcelReader(
      sourceFilePath,
      frontEndFundName,
      MyobGLReader.setting1
    )
    val tableRow2 = BasicExcelReaderHelper.processFileOnExcelReader(
      sourceFilePath,
      frontEndFundName,
      MyobGLReader.setting2
    )
    val tableRow3 = BasicExcelReaderHelper.processFileOnExcelReader(
      sourceFilePath,
      frontEndFundName,
      MyobGLReader.setting3
    )
    val tableRow4 = BasicExcelReaderHelper.processFileOnExcelReader(
      sourceFilePath,
      frontEndFundName,
      MyobGLReader.setting4
    )
    BasicGLProcessor.processFile(
      tableRow1 ++ tableRow2 ++ tableRow3 ++ tableRow4,
      frontEndPeriodStart,
      frontEndPeriodEnd,
      TranDate,
      storage
    )
  }

}
