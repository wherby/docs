def areSheetExisted(filePath: String, sheetsName: Seq[String]): Boolean = {
  val workbook        = AppFileIO.fileToWorkBook(filePath)
  var allSheetExisted = true
  try {
    for (sheetName <- sheetsName) {
      if (workbook.getSheetIndex(sheetName) < 0) {
        allSheetExisted = false
      }
    }
    allSheetExisted
  } finally {
    workbook.close()
  }