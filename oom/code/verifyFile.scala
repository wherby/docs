
private def verifyFileForNoMatchedFormula(fileInfo: FileInfo) = {
  val resultForExcel = downloadService.downloadForFile(Seq(fileInfo))
  val filePath = "conf/Download.xlsx"
  val workbook = new XSSFWorkbook(new FileInputStream(new File(filePath)))
  try{
    downloadService.evaluateExtractedDataToExcel(resultForExcel._1, workbook)
  } catch {
    case ex=>
      println(ex)
  }
  val result = downloadService.evaluateExtractedDataToExcel(resultForExcel._1, workbook)
  workbook.close()
  result
}