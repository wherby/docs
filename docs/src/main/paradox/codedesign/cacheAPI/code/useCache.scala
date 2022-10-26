object ExcelToArray {
  def multiSheetToArray(
                         filePath: String,
                         dataFormatOpt: Option[SimpleDateFormat] = None
                       ): Seq[Seq[Seq[ExcelField]]] = {
    AppCache.getTabValueList(filePath, () => {
      val workbook = AppFileIO.fileToWorkBook(filePath)
      try {
        val num = workbook.getNumberOfSheets
        var res = Seq[Seq[Seq[ExcelField]]]()
        for (i <- 0 until (num)) {
          val t1 = getArryFromSheet(workbook, i, dataFormatOpt)
          res = res :+ t1
        }
        AppCache.putValue(filePath, res)
        res
      } finally {
        workbook.close()
      }
    })
  }

  def excelToArray(
                    filePath: String,
                    dataFormatOpt: Option[SimpleDateFormat] = None,
                    sheetIndex: Option[Int] = None
                  ): Seq[Seq[ExcelField]] = {
    AppCache.getTabValue(filePath + sheetIndex.getOrElse(""),()=>{
      if (filePath.endsWith(".csv")) {
        ExcelReaderHelper.readCSV(filePath)
      } else {
        val workbook = AppFileIO.fileToWorkBook(filePath)
        try {
          val index = sheetIndex.getOrElse(0)
          val res2 = getArryFromSheet(workbook, index, dataFormatOpt)
          removeEmptyEnd(res2)
        } finally {
          workbook.close()
        }
      }
    })
  }

  def excelToArray_tab(
                        filePath: String,
                        dataFormatOpt: Option[SimpleDateFormat] = None,
                        sheetIndex: Option[Int] = None
                      ): Seq[Seq[ExcelField]] = {
    AppCache.getTabValue(filePath + sheetIndex.getOrElse(""),()=>{
      if (filePath.endsWith(".csv")) {
        ExcelReaderHelper.readCSV_tab(filePath)
      } else {
        val workbook = AppFileIO.fileToWorkBook(filePath)
        try {
          val index = sheetIndex.getOrElse(0)
          val res2 = getArryFromSheet(workbook, index, dataFormatOpt)
          removeEmptyEnd(res2)
        } finally {
          workbook.close()
        }
      }
    })
  }
}